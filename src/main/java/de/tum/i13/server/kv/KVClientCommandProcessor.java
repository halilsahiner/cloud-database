package de.tum.i13.server.kv;

import de.tum.i13.server.threadperconnection.serverconnection.ServerSenderSocket;
import de.tum.i13.shared.CommandProcessor;
import de.tum.i13.shared.Config;
import de.tum.i13.shared.HashConverter;
import de.tum.i13.shared.MetadataHandler;
import org.javatuples.Pair;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

public class KVClientCommandProcessor implements CommandProcessor {
    public static Logger logger = Logger.getLogger(KVClientCommandProcessor.class.getName());
    private final ReentrantReadWriteLock dataLock;
    private final KVStoreInterface diskStore;
    private final KVStoreInterface cacheStore;
    private final String cacheStrategy;
    private final AtomicBoolean isServerWriteLocked;
    private final Config config;
    private final ServerSenderSocket serverSenderSocket;

    public KVClientCommandProcessor(ReentrantReadWriteLock dataLock, KVStoreInterface diskStore,
                                    KVStoreInterface cacheStore, String cacheStrategy,
                                    AtomicBoolean isServerWriteLocked, Config config, ServerSenderSocket serverSenderSocket) {
        this.dataLock = dataLock;
        this.diskStore = diskStore;
        this.cacheStore = cacheStore;
        this.cacheStrategy = cacheStrategy;
        this.isServerWriteLocked = isServerWriteLocked;
        this.config = config;
        this.serverSenderSocket = serverSenderSocket;
    }

    @Override
    public String process(String command) {
        String[] words = command.trim().split("\\s+", 3);
        command = words[0];
        // if the metadata is empty, server haven't initialized yet.
        if (command.equalsIgnoreCase("keyrange")) {
            return new Message(null, MetadataHandler.getKeyrange(), KVMessage.StatusType.KEY_RANGE_SUCCESS).toString();
        } else if (command.equalsIgnoreCase("keyrange_read")) {
            return new Message(null, MetadataHandler.getKeyrangeRead(), KVMessage.StatusType.KEY_RANGE_READ_SUCCESS).toString();
        } else {
            String key = words[1];
            Pair<String, String> readRange = MetadataHandler.getReadRange();
            Pair<String, String> coordinatedRange = MetadataHandler.getCoordinatedRange();
            boolean isInReadRange = HashConverter.isInKeyRange(readRange, key);
            boolean isInCoordinatedRange = HashConverter.isInKeyRange(coordinatedRange, key);

            if (!isInReadRange) {
                //logger.warning("Problem with the key" + readRange + " " + key);
                //logger.info(HashConverter.getMd5(key) + " key is not in this range: " + readRange + "\n" + MetadataHandler.getMetadata());
                return new Message(KVMessage.StatusType.SERVER_NOT_RESPONSIBLE).toString();
            }
            // if server is not stopped, it should process get commands.
            else if (command.equalsIgnoreCase("get")) {
                return getHelper(key).toString();
            } else if (!isInCoordinatedRange) {
                // If the command is put or delete, the key should be in coordinated range. Otherwise, kv-server is not
                // allowed to change the replicated data.
                //logger.warning("Problem with the key" + coordinatedRange + " " + key);
                //logger.info(HashConverter.getMd5(key) + " key is not in this range: " + coordinatedRange + "\n" + MetadataHandler.getMetadata());
                return new Message(KVMessage.StatusType.SERVER_NOT_RESPONSIBLE).toString();
            } else if (isServerWriteLocked.get()) {
                // if there is a lock on metadata, i.e. -> there is a reallocation of nodes
                // or if there is a data exchange btw two KVServers
                return new Message(KVMessage.StatusType.SERVER_WRITE_LOCK).toString();
            }
            // If no write lock present on server, put and delete can be processed
            else if (command.equalsIgnoreCase("put")) {
                String value = words[2];
                return putHelper(key, value).toString();
            } else if (command.equalsIgnoreCase("delete")) {
                return deleteHelper(key).toString();
            }
        }

        return "Error! This command is invalid!";

    }

    @Override
    public String connectionAccepted(InetSocketAddress address, InetSocketAddress remoteAddress) {
        logger.info("New client connection: " + remoteAddress.toString());
        return "connected_ack\r\n";
    }

    @Override
    public void connectionClosed(InetAddress remoteAddress) {
        logger.info("Client connection closed: " + remoteAddress.toString());
    }

    /**
     * This method has critical section. Thread that
     * calls this function gets writeLock for delete operation.
     * Then, it processes delete operation and update
     * the cache.
     *
     * @param key the key that identifies the value.
     * @return a KVMessage object that indicates message of
     * get operation.
     */
    private KVMessage deleteHelper(String key) {
        KVMessage diskMessage = new Message(key, null, KVMessage.StatusType.DELETE_ERROR);
        KVMessage cacheMessage;
        try {
            dataLock.writeLock().lock();
            diskMessage = diskStore.delete(key);
            cacheMessage = cacheStore.delete(key);
            if (diskMessage.getStatus() == KVMessage.StatusType.DELETE_SUCCESS && cacheMessage.getStatus() == KVMessage.StatusType.DELETE_ERROR) {
                cacheStore.put(key, null);
            }
        } finally {
            //release the lock
            dataLock.writeLock().unlock();
            if (diskMessage.getStatus() == KVMessage.StatusType.DELETE_SUCCESS) {
                // if the delete process is successful in coordinator, the replicas should be updated accordingly.
                updateReplicas("delete", key, "");
            }
        }
        return diskMessage;
    }

    /**
     * This method has critical section. Thread that
     * calls this function gets writeLock for put operations.
     * Then, it processes put or update operations
     * and update the cache.
     *
     * @param key   commands come from client side.
     * @param value the value that is indexed by the given key.
     * @return a KVMessage object that indicates message of
     * put/update/delete operation.
     */
    private KVMessage putHelper(String key, String value) {
        KVMessage diskMessage;
        try {
            // get the lock
            dataLock.writeLock().lock();
            cacheStore.put(key, value);
            diskMessage = diskStore.put(key, value);
        } finally {
            //release the lock
            dataLock.writeLock().unlock();
            updateReplicas("put", key, value);
        }
        return diskMessage;
    }

    /**
     * This method has critical section. Thread that
     * calls this function gets readLock for get operation.
     * Then, it processes get operation and update
     * the cache.
     *
     * @param key the key that identifies the value.
     * @return a KVMessage object that indicates message of
     * get operation.
     */
    private KVMessage getHelper(String key) {
        KVMessage message;

        logger.info("Received get messsage for key : " + key);
        try {
            // get the lock
            if (cacheStrategy.startsWith("L")) {
                dataLock.writeLock().lock();
            } else {
                dataLock.readLock().lock();
            }
            message = cacheStore.get(key);
            logger.info("Received cache message: " + message.getStatus().toString() + " key: " + message.getKey());
            if (message.getStatus() != KVMessage.StatusType.GET_SUCCESS && message.getStatus() != KVMessage.StatusType.GET_DELETED) {
                logger.info("Disk search for key: " + message.getKey());
                message = diskStore.get(key);
                logger.info("Received disk message: " + message.getStatus().toString());
                if (message.getStatus() == KVMessage.StatusType.GET_SUCCESS) {
                    cacheStore.put(message.getKey(), message.getValue());
                }
            }
        } finally {
            //release the lock
            if (cacheStrategy.startsWith("L")) {
                dataLock.writeLock().unlock();
            } else {
                dataLock.readLock().unlock();
            }
        }
        return message;
    }

    private void updateReplicas(String operation, String key, String value) {
        String coordinatedServer1 = MetadataHandler.getCoordinatedServer1();
        String coordinatedServer2 = MetadataHandler.getCoordinatedServer2();
        if (coordinatedServer1 != null && coordinatedServer2 != null) {
            serverSenderSocket.sendSingleKV(coordinatedServer1, operation, key, value);
            serverSenderSocket.sendSingleKV(coordinatedServer2, operation, key, value);
        }
    }
}
