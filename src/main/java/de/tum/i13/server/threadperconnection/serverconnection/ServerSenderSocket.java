package de.tum.i13.server.threadperconnection.serverconnection;

import de.tum.i13.server.kv.disk.DataDiskStore;
import de.tum.i13.shared.Config;
import de.tum.i13.shared.Constants;
import org.javatuples.Pair;
import org.javatuples.Quartet;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

public class ServerSenderSocket {

    private final static Logger logger = Logger.getLogger(ServerSenderSocket.class.getName());
    private final ReentrantReadWriteLock dataLock;
    private final Config cfg;
    private final DataDiskStore diskStore;


    public ServerSenderSocket(Config cfg, ReentrantReadWriteLock dataLock, DataDiskStore diskStore) {
        this.dataLock = dataLock;
        this.cfg = cfg;
        this.diskStore = diskStore;
    }

    /**
     * Creates a connection with another KV-Server which address given through parameter and sends the KV pairs
     * between the given range.
     *
     * @param address    KV-Server address to connect
     * @param rangeStart Start of transferred key range
     * @param rangeEnd   End of transferred key range
     * @return true if transfer is complete
     */
    public void transfer(String address, String rangeStart, String rangeEnd, PrintWriter ecsOut, String message) {
        new Thread(() -> {
            try {
                logger.info("Trying to connect: " + address);
                Socket serverSocket = new Socket(address.split(":")[0], cfg.ServerCommunicationPort);

                BufferedReader in = new BufferedReader(new InputStreamReader(serverSocket.getInputStream(), Constants.TELNET_ENCODING));
                PrintWriter out = new PrintWriter(new OutputStreamWriter(serverSocket.getOutputStream(), Constants.TELNET_ENCODING));
                String firstLine;

                logger.info("Connection successful!");
                dataLock.readLock().lock();
                Pair<ArrayList<Quartet<Integer, String, Integer, Integer>>, Integer> nextSetOfKeyInfoInRange = new Pair<>(new ArrayList<>(), diskStore.getNumberOfFileGroups());
                logger.info("Waiting for the server response");
                while ((firstLine = in.readLine()) != null) {
                    if (firstLine.startsWith("connected_ack")) {
                        firstLine = firstLine.substring("connected_ack".length());
                        break;
                    }
                }
                logger.info("Server response is acquired.");
                do {
                    nextSetOfKeyInfoInRange = diskStore.getNextSetOfKeyInfoInRange(rangeStart, rangeEnd, nextSetOfKeyInfoInRange.getValue1());
                    for (Quartet<Integer, String, Integer, Integer> keyInfo : nextSetOfKeyInfoInRange.getValue0()) {
                        String value = diskStore.readFromAPosition(cfg.dataDir + "/" + keyInfo.getValue0() + Constants.VALUE_EXTENSION,
                                keyInfo.getValue2(), keyInfo.getValue3());

                        String putCommand = "put " + keyInfo.getValue1() + " " + value + "\r\n";
                        out.write(putCommand);
                        out.flush();

                        while ((firstLine = in.readLine()) != null) {
                            // if the kv pair is successfully transferred, we can try to send another one
                            if (firstLine.equalsIgnoreCase("put_success " + keyInfo.getValue1()) ||
                                    firstLine.equalsIgnoreCase("put_update " + keyInfo.getValue1())) {
                                break;
                            } else {
                                // if it is not successful, we can try again until getting a success
                                out.write(putCommand);
                                out.flush();
                            }
                        }
                    }
                } while (nextSetOfKeyInfoInRange.getValue0().size() != 0);
                dataLock.readLock().unlock();
                out.close();
                in.close();
                serverSocket.close();
                if(ecsOut != null) {
                    ecsOut.write(message);
                    ecsOut.flush();
                }
            } catch (UnknownHostException e) {
                logger.info("The connection address or port number is wrong.");
                logger.severe(e.getMessage());
                if(ecsOut != null) {
                    ecsOut.write("transfer_init_error\r\n");
                    ecsOut.flush();
                    logger.info("Error init!");
                }
            } catch (IOException e) {
                logger.warning("Connection failed.");
                logger.severe(e.getMessage());
                if(ecsOut != null) {
                    ecsOut.write("transfer_init_error\r\n");
                    ecsOut.flush();
                    logger.info("Error init!");
                }
            }
        }).start();

    }

    public void replicaTransfer(String coordinatedServer1, String coordinatedServer2, String rangeStart, String rangeEnd) {
        Thread th = new Thread(() -> {
            if (coordinatedServer1 != null)
                transfer(coordinatedServer1, rangeStart, rangeEnd, null, "");
            if (coordinatedServer2 != null)
                transfer(coordinatedServer2, rangeStart, rangeEnd,null, "");
        });
        th.start();
    }

    public void sendSingleKV(String address, String operation, String key, String value) {
        Thread th = new Thread(() -> {
            try {
                logger.info("Trying to connect: " + address);
                Socket serverSocket = new Socket(address.split(":")[0], cfg.ServerCommunicationPort);
                BufferedReader in = new BufferedReader(new InputStreamReader(serverSocket.getInputStream(), Constants.TELNET_ENCODING));
                PrintWriter out = new PrintWriter(new OutputStreamWriter(serverSocket.getOutputStream(), Constants.TELNET_ENCODING));
                String firstLine;
                logger.info("Connection successful!");

                String command = operation + " " + key + " " + value + "\r\n";
                out.write(command);
                out.flush();

                while ((firstLine = in.readLine()) != null) {
                    // if the kv pair is successfully transferred, we can try to send another one
                    if (firstLine.startsWith("connected_ack")) {
                        firstLine = firstLine.substring("connected_ack".length());
                    }

                    if (firstLine.equalsIgnoreCase("put_success " + key) ||
                            firstLine.equalsIgnoreCase("put_update " + key) ||
                            firstLine.equalsIgnoreCase("delete_success " + key)) {
                        break;
                    } else {
                        // if it is not successful, we can try again until getting a success
                        out.write(command);
                        out.flush();
                    }
                }

                in.close();
                out.close();
                serverSocket.close();
            } catch (UnknownHostException e) {
                logger.info("The connection address or port number is wrong.");
                logger.severe(e.getMessage());
                //return false;
            } catch (IOException e) {
                logger.warning("Connection failed.");
                logger.severe(e.getMessage());
                //return false;
            }
        });

        th.start();
    }

    public boolean delete(boolean isShutDown, String rangeStart, String rangeEnd) {
        if (isShutDown) {
            diskStore.deleteAll();
            return true;
        } else {
//            try {
//                dataLock.readLock().lock();
//                Pair<ArrayList<Quartet<Integer, String, Integer, Integer>>, Integer> nextSetOfKeyInfoInRange = new Pair<>(new ArrayList<>(), diskStore.getNumberOfFileGroups());
//
//                do {
//                    nextSetOfKeyInfoInRange = diskStore.getNextSetOfKeyInfoInRange(rangeStart, rangeEnd, nextSetOfKeyInfoInRange.getValue1());
//                    for (Quartet<Integer, String, Integer, Integer> keyInfo : nextSetOfKeyInfoInRange.getValue0()) {
//
//                        diskStore.deleteKey(keyInfo.getValue1());
//                    }
//                } while (nextSetOfKeyInfoInRange.getValue0().size() != 0);
//
//                dataLock.readLock().unlock();
//                return true;
//            } catch (IOException e) {
//                logger.severe("Value can not be added/updated. Caused by: " + e.getMessage());
//                return false;
//            }
            return true;
        }
    }

}
