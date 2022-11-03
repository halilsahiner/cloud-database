package de.tum.i13.ecs;

import de.tum.i13.shared.Constants;
import de.tum.i13.shared.MetadataHandler;
import org.javatuples.Pair;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class KVServerConnectionHandler implements Runnable {
    private final static Logger logger = Logger.getLogger(KVServerConnectionHandler.class.getName());
    private final Socket clientSocket;
    private final ECSServer ecsServer;
    private final BlockingQueue<String> initFinished;
    private final BlockingQueue<String> killFinished;
    private final BlockingQueue<String> isECSShutdownDone;
    private final AtomicBoolean isECSShuttingDown;

    public KVServerConnectionHandler(Socket clientSocket, ECSServer ecsServer, BlockingQueue<String> initFinished, BlockingQueue<String> killFinished, BlockingQueue<String> isECSShutdownDone, AtomicBoolean isECSShuttingDown) {
        this.clientSocket = clientSocket;
        this.ecsServer = ecsServer;
        this.initFinished = initFinished;
        this.killFinished = killFinished;
        this.isECSShutdownDone = isECSShutdownDone;
        this.isECSShuttingDown = isECSShuttingDown;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), Constants.TELNET_ENCODING));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), Constants.TELNET_ENCODING))) {
            logger.info(clientSocket.getRemoteSocketAddress() + " is connected to ECS");
            String address = "";
            String firstLine;

            while ((firstLine = in.readLine()) != null) {
                if (firstLine.startsWith("server_address") && !isECSShuttingDown.get()) {
                    initFinished.put("New Server");
                    address = firstLine.split("\\s+")[1];
                    ecsServer.putKVServerToRing(address);
                    ecsServer.addKVServerToClientConnections(address, out);
                    sendMessageToKVServer(out, "keyrange_success " + ecsServer.getKeyRanges());
                    if (ecsServer.ringSize() != 1) {
                        ecsServer.initializationDataTransfer(address);
                    } else {
                        initFinished.take();
                    }
                } else if (firstLine.equals("transfer_init_completed")) {
                    ecsServer.sendKeyRangesToAll();
                    logger.info("transfer_init_completed before initFinished");
                    sendMessageToKVServer(out, "set_write_unlock\r\n");
                    initFinished.take();
                    if (isECSShuttingDown.get()) {
                        isECSShutdownDone.put("Shut down can be taken");
                    }
                    logger.info("set_write_unlock transfer_init_completed");
                } else if (firstLine.equals("transfer_kill_completed")) {
                    ecsServer.sendKeyRangesToAll();
                    sendMessageToKVServer(out, "set_write_unlock\r\n");
                    logger.info("Shutdown end " + System.currentTimeMillis());
                    killFinished.take();
                    if (isECSShuttingDown.get()) {
                        isECSShutdownDone.put("Shut down can be taken");
                    }
                } else if (firstLine.equals("shut_down")) {
                    logger.info("SHUT DOWN RECEIVED");
                    if (isECSShuttingDown.get()) {
                        sendMessageToKVServer(out, "ecs_shutting_down\r\n");
                    } else {
                        logger.info("Shutdown start " + System.currentTimeMillis());
                        killFinished.put("Shut down start");
                        String successorAddress = ecsServer.getSuccessorAddress(address);
                        Pair<String, String> range = ecsServer.removeKVServerFromRing(address);
                        if (ecsServer.ringSize() != 0) {
                            sendMessageToKVServer(out, "set_write_lock\r\n");
                            ecsServer.sendMetadataToSuccessor(successorAddress);
                            sendMessageToKVServer(out, ecsServer.removalDataTransfer(range, successorAddress));
                        } else {
                            sendMessageToKVServer(out, "set_write_lock\r\n");
                            sendMessageToKVServer(out, ecsServer.removalDataTransfer(range, "self"));
                        }
                        ecsServer.removeKVServerToClientConnections(address);
                    }

                } else if (firstLine.equals("heartbeat_back")) {
                    ecsServer.getHeartbeats().put(address);
                } else if (firstLine.equals("ping_heartbeat_ecs")) {
                    //logger.info("heartbeat_ecs_back");
                    sendMessageToKVServer(out, "heartbeat_ecs_back\r\n");
                } else if (firstLine.equals("remove_block_ecs")) {
                    logger.info("remove_block_ecs_start");
                    while (!ecsServer.isPreviousServersConnected()){
                    }
                    logger.info("remove_block_ecs_sent");
                    ecsServer.sendRemoveBlockElectionToAll();
                }
            }
            in.close();
            out.close();
            clientSocket.close();
        } catch (IOException | InterruptedException exception) {
            logger.warning("One of the server connections with ECS throw an exception!");
        }

    }

    private void sendMessageToKVServer(PrintWriter out, String message) {
        out.write(message);
        out.flush();
    }
}
