package de.tum.i13.server.threadperconnection;

import de.tum.i13.client.CommandLineInterface;
import de.tum.i13.ecs.ECSServerMain;
import de.tum.i13.server.kv.*;
import de.tum.i13.server.kv.cache.DataCacheFIFOStore;
import de.tum.i13.server.kv.cache.DataCacheLFUStore;
import de.tum.i13.server.kv.cache.DataCacheLRUStore;
import de.tum.i13.server.kv.disk.DataDiskStore;
import de.tum.i13.server.threadperconnection.clientconnection.ClientSocketHandler;
import de.tum.i13.server.threadperconnection.ecsconnection.ElectionServerSocketHandler;
import de.tum.i13.server.threadperconnection.serverconnection.ServerReceiverSocketHandler;
import de.tum.i13.server.threadperconnection.serverconnection.ServerSenderSocket;
import de.tum.i13.shared.CommandProcessor;
import de.tum.i13.shared.Config;
import de.tum.i13.shared.MetadataHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

import static de.tum.i13.client.Constants.HELP_MESSAGE_SERVER;
import static de.tum.i13.shared.Config.parseCommandlineArgs;
import static de.tum.i13.shared.LogSetup.setupLogging;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());
    private static final AtomicBoolean isServerWorking = new AtomicBoolean(false);
    private static final AtomicBoolean isServerWriteLocked = new AtomicBoolean(false);
    private static final AtomicBoolean isECSShuttingDown = new AtomicBoolean(false);
    private static final BlockingQueue<String> isECSShutdownDone = new ArrayBlockingQueue<>(1);
    private static final BlockingQueue<String> initFinished = new ArrayBlockingQueue<>(1);
    private static final BlockingQueue<String> killFinished = new ArrayBlockingQueue<>(1);
    private static final BlockingQueue<String> sendShutDown = new ArrayBlockingQueue<>(1);
    private static final BlockingQueue<String> isShutDownDone = new ArrayBlockingQueue<>(1);
    private static Config config;

    private static ServerSocket ecsServerSocket;

    public static void main(String[] args) throws IOException {
        config = parseCommandlineArgs(args);
        if (config.usagehelp) {
            System.out.println(HELP_MESSAGE_SERVER);
            return;
        }
        setupLogging(config.logfile);

        ReentrantReadWriteLock dataLock = new ReentrantReadWriteLock();
        DataDiskStore diskStore = new DataDiskStore(config, logger);
        ServerSenderSocket serverSenderSocket = new ServerSenderSocket(config, dataLock, diskStore);

        CommandLineInterface cli = new CommandLineInterface();
        cli.changeLogLevel(config.logLevel, logger);
        final ServerSocket kvClientSocket = new ServerSocket();
        final ServerSocket kvServerSocket = new ServerSocket();

        // KVClient - KVServer port for communication
        logger.info("Client-Server socket bound to address " + config.listenaddr + " " + config.port);
        kvClientSocket.bind(new InetSocketAddress(config.listenaddr, config.port));

        // KVServer - KVServer port for communication btw KVServers
        kvServerSocket.bind(new InetSocketAddress(config.listenaddr, 3000));
        logger.info("Server-Server socket bound to address " + config.listenaddr + " " + 3000);

        // Replace with your Key value server logic.
        // If you use multithreading you need locking
        KVStoreInterface cacheStore;

        switch (config.cacheStrategy) {
            case ("FIFO"):
                cacheStore = new DataCacheFIFOStore(config.sizeOfCache);
                break;
            case ("LRU"):
                cacheStore = new DataCacheLRUStore(config.sizeOfCache);
                break;
            case ("LFU"):
                cacheStore = new DataCacheLFUStore(config.sizeOfCache);
                break;
            default:
                logger.warning("Not a valid cache replacement strategy");
                return;
        }

        CommandProcessor clientLogic = new KVClientCommandProcessor(dataLock, diskStore, cacheStore, config.cacheStrategy, isServerWriteLocked, config, serverSenderSocket);
        CommandProcessor serverLogic = new KVServerCommandProcessor(dataLock, diskStore, cacheStore);

        new Thread(new ClientSocketHandler(kvClientSocket, clientLogic, isServerWorking)).start();
        new Thread(new ServerReceiverSocketHandler(kvServerSocket, serverLogic, isServerWorking)).start();

        ECSServerHandler ecsServerHandler = new ECSServerHandler(config, serverSenderSocket, isServerWorking,
                isServerWriteLocked, sendShutDown, isShutDownDone, initFinished, killFinished, isECSShutdownDone,
                isECSShuttingDown);

        logger.info("KV-Server initialized at : " + config.listenaddr);

        String ecsIP = ecsServerHandler.findECSServer();
        MetadataHandler.setEcsIP(ecsIP);

        if (ecsIP.equals(config.listenaddr)) {
            logger.info(config.listenaddr + " is the ECS!");
            try {
                ecsServerSocket = new ServerSocket();
                ecsServerSocket.bind(new InetSocketAddress(config.listenaddr, config.ECSCommunicationPort));
                logger.info("ECS Socket bound to address " + config.listenaddr + " " + config.ECSCommunicationPort);
                new Thread(new ECSServerMain(initFinished, killFinished, isECSShutdownDone, isECSShuttingDown, config, ecsServerSocket, new ArrayList<>())).start();
                ecsServerHandler.connectToECSHelper();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            ecsServerHandler.connectToECSHelper();
            ecsServerHandler.startECSPingMessages();
        }
        electionServerSocketHelper(ecsServerHandler);

        Runtime.getRuntime().addShutdownHook(new Thread(() ->
        {
            try {
                logger.info("Shut down steps initialization");
                sendShutDown.put("Shut down steps initialization");
                isShutDownDone.take();

                // If the kv server is the ECS, the kv-server should pass down the ECS role to the successor
                logger.info("ECS shutting down is starting");
                isECSShuttingDown.set(true);

                if (!killFinished.isEmpty() || !initFinished.isEmpty()) {
                    logger.info("Unfinished kill" + killFinished.isEmpty() + "or init" + initFinished.isEmpty());
                    isECSShutdownDone.take();
                }

                logger.info("Shut down take");

                System.out.println("Closing KVServer - KVClient sockets");
                kvClientSocket.close();

                System.out.println("Closing KVServer - KVServer sockets");
                kvServerSocket.close();

                if(ecsServerSocket != null) {
                    System.out.println("Closing ECSServer - KVServer sockets");
                    ecsServerSocket.close();
                }

            } catch (IOException | InterruptedException e) {
                logger.warning("Server is not closed properly!");
            }
        }));
    }

    private static void electionServerSocketHelper(ECSServerHandler ecsServerHandler) {
        try {
            CommandProcessor ecsLogic = new KVElectionCommandProcessor(config, ecsServerHandler);
            ecsServerHandler.setECSElectionServerSocket(new ServerSocket());
            ecsServerHandler.getECSElectionServerSocket().bind(new InetSocketAddress(config.listenaddr, config.ElectionCommunicationPort));
            logger.info("Server-Server ECS communication socket bound " + config.ElectionCommunicationPort);

            new Thread(new ElectionServerSocketHandler(ecsServerHandler.getECSElectionServerSocket(), ecsLogic, isServerWorking)).start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
