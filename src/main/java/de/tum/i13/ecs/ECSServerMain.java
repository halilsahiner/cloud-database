package de.tum.i13.ecs;

import de.tum.i13.shared.Config;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import static de.tum.i13.shared.LogSetup.setupLogging;

public class ECSServerMain extends Thread {
    private final static Logger logger = Logger.getLogger(ECSServerMain.class.getName());
    private final BlockingQueue<String> initFinished;
    private final BlockingQueue<String> killFinished;
    private final BlockingQueue<String> isECSShutdownDone;

    private final AtomicBoolean isECSShuttingDown;
    private final Config cfg;

    private final ServerSocket serverSocket;
    private final List<String> previousMetadata;



    public ECSServerMain(BlockingQueue<String> initFinished, BlockingQueue<String> killFinished, BlockingQueue<String> isECSShutdownDone, AtomicBoolean isECSShuttingDown, Config cfg, ServerSocket serverSocket, List<String> previousMetadata) {
        this.initFinished = initFinished;
        this.killFinished = killFinished;
        this.isECSShutdownDone = isECSShutdownDone;
        this.isECSShuttingDown = isECSShuttingDown;
        this.cfg = cfg;
        this.serverSocket = serverSocket;
        this.previousMetadata = previousMetadata;
    }

    @Override
    public void run() {
        Path logDirectory = Paths.get("src", "/ecs.log");
        setupLogging(logDirectory);

        try {
            ECSServer ecsServer = new ECSServer(previousMetadata);
            ecsServer.sendHeartbeats(cfg.listenaddr);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                logger.info("New server : " + clientSocket.getRemoteSocketAddress());
                Thread th = new Thread(new KVServerConnectionHandler(clientSocket, ecsServer, initFinished, killFinished, isECSShutdownDone, isECSShuttingDown));
                th.start();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
