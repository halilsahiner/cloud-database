package de.tum.i13.server.threadperconnection.serverconnection;

import de.tum.i13.server.threadperconnection.ConnectionHandleThread;
import de.tum.i13.shared.CommandProcessor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class ServerReceiverSocketHandler implements Runnable {

    private static final Logger logger = Logger.getLogger(ServerReceiverSocketHandler.class.getName());
    private final ServerSocket socket;
    private final CommandProcessor logic;
    private final AtomicBoolean isServerWorking;

    public ServerReceiverSocketHandler(ServerSocket socket, CommandProcessor logic, AtomicBoolean isServerWorking) {
        this.socket = socket;
        this.logic = logic;
        this.isServerWorking = isServerWorking;
    }

    @Override
    public void run() {
        while (true) {
            Socket serverSocket = null;
            try {
                serverSocket = socket.accept();
                logger.info("Message received in server-server listener. Creating handler thread.");
            } catch (IOException e) {
                logger.warning("Exception occurred while handling server-server connection");
                logger.warning(e.toString());
            }
            Thread th = new ConnectionHandleThread(logic, serverSocket, isServerWorking);
            th.start();
        }
    }
}

