package de.tum.i13.server.threadperconnection.ecsconnection;

import de.tum.i13.server.threadperconnection.serverconnection.ServerSenderSocket;
import de.tum.i13.shared.Config;
import de.tum.i13.shared.Constants;
import de.tum.i13.shared.MetadataHandler;
import org.javatuples.Pair;

import java.io.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class ECSConnectionHandler extends Thread {
    private static final Logger logger = Logger.getLogger(ECSConnectionHandler.class.getName());
    private final AtomicBoolean isServerWorking;
    private final AtomicBoolean isServerWriteLocked;
    private final BlockingQueue<String> sendShutDown;
    private final BlockingQueue<String> isShutDownDone;
    private final Config cfg;
    private final ServerSenderSocket serverSenderSocket;
    private final AtomicBoolean isEcsAlive;
    public final PrintWriter ECSServerOut;
    public final BufferedReader ECSServerIn;
    private final AtomicBoolean blockElection;

    public ECSConnectionHandler(AtomicBoolean isServerWorking, BlockingQueue<String> sendShutDown, BlockingQueue<String> isShutDownDone,
                                AtomicBoolean isServerWriteLocked, Config cfg, ServerSenderSocket serverSenderSocket, AtomicBoolean isEcsAlive, PrintWriter ECSServerOut, BufferedReader ECSServerIn, AtomicBoolean blockElection) {
        this.isServerWorking = isServerWorking;
        this.sendShutDown = sendShutDown;
        this.isShutDownDone = isShutDownDone;
        this.isServerWriteLocked = isServerWriteLocked;
        this.cfg = cfg;
        this.serverSenderSocket = serverSenderSocket;
        this.isEcsAlive = isEcsAlive;
        this.ECSServerOut = ECSServerOut;
        this.ECSServerIn = ECSServerIn;
        this.blockElection = blockElection;
    }

    @Override
    public void run() {
        Thread shutDownThread = listenShutDown(ECSServerOut);
        shutDownThread.start();
        try {
            String firstLine;
            logger.info("Send : " + "server_address " + cfg.listenaddr + ":" + cfg.port + "\r\n");
            ECSServerOut.write("server_address " + cfg.listenaddr + ":" + cfg.port + "\r\n");
            ECSServerOut.flush();

            boolean isShutDown = false;
            String rangeStart = "", rangeEnd = "";
            while ((firstLine = ECSServerIn.readLine()) != null) {
                if (firstLine.startsWith("keyrange_success")) {
                    logger.info("keyrange: " + firstLine);
                    MetadataHandler.updateMetadata(firstLine.split("\\s+")[1], cfg.listenaddr + ":" + cfg.port);
                    isServerWorking.set(true);
                } else if (firstLine.startsWith("update_ranges")) {
                    logger.info("update_ranges: " + firstLine);
                    String previousCoordinatedServer1 = MetadataHandler.getCoordinatedServer1();
                    String previousCoordinatedServer2 = MetadataHandler.getCoordinatedServer2();
                    MetadataHandler.updateMetadata(firstLine.split("\\s+")[1], cfg.listenaddr + ":" + cfg.port);
                    // Send the coordinated data to new replicas
                    replicaUpdateHelper(previousCoordinatedServer1, previousCoordinatedServer2);
                } else if (firstLine.equals("set_write_lock")) {
                    logger.info("set_write_lock");
                    isServerWriteLocked.set(true);
                } else if (firstLine.equals("set_write_unlock")) {
                    logger.info("set_write_unlock");
                    if (serverSenderSocket.delete(isShutDown, rangeStart, rangeEnd)) {
                        if (isShutDown) {
                            logger.info("Completed kill!");
                            isShutDownDone.put("Shut down completed");
                        }
                    } else {
                        logger.info("Error delete!");
                    }
                    isServerWriteLocked.set(false);
                } else if (firstLine.startsWith("transfer_data_init")) {
                    String[] transferServerInfo = firstLine.split("\\s+")[1].split(";");
                    rangeStart = transferServerInfo[1];
                    rangeEnd = transferServerInfo[2];
                    serverSenderSocket.transfer(transferServerInfo[0], rangeStart, rangeEnd, ECSServerOut, "transfer_init_completed\r\n");
                } else if (firstLine.startsWith("transfer_data_kill")) {
                    String[] transferServerInfo = firstLine.split("\\s+")[1].split(";");
                    String address = transferServerInfo[0];
                    rangeStart = transferServerInfo[1];
                    rangeEnd = transferServerInfo[2];
                    isShutDown = true;
                    if (address.equals("self")) {
                        ECSServerOut.write("transfer_kill_completed\r\n");
                        ECSServerOut.flush();
                        logger.info("Completed kill!");
                    } else {
                        serverSenderSocket.transfer(transferServerInfo[0], rangeStart, rangeEnd, ECSServerOut, "transfer_kill_completed\r\n");
                    }
                } else if (firstLine.equals("ping_heartbeat")) {
                    //logger.info("HEARTBEAT FOR ECS: " + MetadataHandler.getEcsIP());
                    ECSServerOut.write("heartbeat_back\r\n");
                    ECSServerOut.flush();
                } else if (firstLine.equals("heartbeat_ecs_back")){
                    isEcsAlive.set(true);
                } else if(firstLine.equals("ecs_shutting_down")){
                    logger.info("ECS is shutting down, unhandled graceful shutdown case!!");
                }else if (firstLine.equals("remove_block")){
                    blockElection.set(false);
                    logger.info("REMOVE BLOCK");
                }
            }
        } catch (IOException | InterruptedException ex) {
            logger.info("Connection closed with ECS!");
        }
        shutDownThread.interrupt();
    }

    private Thread listenShutDown(PrintWriter ECSServerOut) {
        return new Thread(() -> {
            try {
                logger.info("LISTEN SHUT DOWN BEGIN");
                logger.info("Send shutdown object id" + sendShutDown.hashCode());
                sendShutDown.take();
                logger.info("LISTEN SHUT DOWN END");
                ECSServerOut.write("shut_down\r\n");
                ECSServerOut.flush();

            } catch (InterruptedException e) {
                logger.warning("Shutdown of the server is not handled properly!");
            }
        });
    }

    private void replicaUpdateHelper(String previousCoordinatedServer1, String previousCoordinatedServer2) {
        String coordinatedServer1 = MetadataHandler.getCoordinatedServer1();
        String coordinatedServer2 = MetadataHandler.getCoordinatedServer2();
        Pair<String, String> coordinatedRange = MetadataHandler.getCoordinatedRange();
        // If the replicas doesn't change with the new update, there shouldn't be a transfer with these checks.
        if (coordinatedServer1 != null && coordinatedServer1.equals(previousCoordinatedServer1))
            coordinatedServer1 = null;
        if (coordinatedServer2 != null && coordinatedServer2.equals(previousCoordinatedServer2))
            coordinatedServer2 = null;

        serverSenderSocket.replicaTransfer(coordinatedServer1, coordinatedServer2,
                coordinatedRange.getValue0(), coordinatedRange.getValue1());
    }
}
