package de.tum.i13.server.kv;

import de.tum.i13.shared.CommandProcessor;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

public class KVServerCommandProcessor implements CommandProcessor {

    public static Logger logger = Logger.getLogger(KVServerCommandProcessor.class.getName());
    private final ReentrantReadWriteLock dataLock;
    private final KVStoreInterface diskStore;
    private final KVStoreInterface cacheStore;

    public KVServerCommandProcessor(ReentrantReadWriteLock dataLock, KVStoreInterface diskStore, KVStoreInterface cacheStore) {
        this.diskStore = diskStore;
        this.dataLock = dataLock;
        this.cacheStore = cacheStore;
    }

    /**
     * @param command command comes from other servers.
     * @return the message that is created after the command is processed.
     */
    @Override
    public String process(String command) {
        String[] words = command.trim().split("\\s+", 3);
        command = words[0];
        KVMessage message = new Message(null, null, KVMessage.StatusType.PUT_ERROR);

        if (command.equalsIgnoreCase("put")) {
            String key = words[1];
            String value = words[2];
            message = putHelper(key, value);
        } else if (command.equalsIgnoreCase("delete")) {
            String key = words[1];
            message = deleteHelper(key);
        }
        //logger.info("Returns message : " + message.toString());
        return message.toString();
    }

    /**
     * This method has critical section. Thread that
     * calls this function gets writeLock for put operations.
     * Then, it processes put operations that are sent out from
     * other server.
     *
     * @param key   commands come from server side.
     * @param value the value that is indexed by the given key.
     * @return a KVMessage object that indicates message of
     * put operation.
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
        }
        return diskMessage;
    }

    /**
     * This method has critical section. Thread that
     * calls this function gets writeLock for delete operation.
     * Then, it processes delete operation.
     *
     * @param key the key that identifies the value.
     * @return a KVMessage object that indicates message of
     * get operation.
     */
    private KVMessage deleteHelper(String key) {
        KVMessage diskMessage;
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
        }
        return diskMessage;
    }

    /**
     * @param address       the address of the endpoint this socket is bound to.
     * @param remoteAddress the address of the endpoint this socket is connected to.
     * @return Reply message which will be sent to connected server
     */
    @Override
    public String connectionAccepted(InetSocketAddress address, InetSocketAddress remoteAddress) {
        logger.info("New server connection: " + remoteAddress.toString());
        return "connected_ack\r\n";
    }

    /**
     * @param remoteAddress the local address to which the socket is bound.
     */
    @Override
    public void connectionClosed(InetAddress remoteAddress) {
        logger.info("Server connection closed: " + remoteAddress.toString());
    }
}
