package de.tum.i13.server.kv;

import de.tum.i13.server.threadperconnection.ecsconnection.ECSConnectionHandler;
import de.tum.i13.server.threadperconnection.serverconnection.ServerSenderSocket;
import de.tum.i13.shared.Config;
import de.tum.i13.shared.Constants;
import de.tum.i13.shared.HashConverter;
import de.tum.i13.shared.MetadataHandler;
import org.awaitility.core.ConditionTimeoutException;
import org.javatuples.Triplet;
import picocli.CommandLine;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import static org.awaitility.Awaitility.await;

public class ECSServerHandler {
    private static final Logger logger = Logger.getLogger(ECSServerHandler.class.getName());
    private final Config config;
    public final ServerSenderSocket serverSenderSocket;
    public final AtomicBoolean isServerWorking;
    public final AtomicBoolean isServerWriteLocked;
    public final BlockingQueue<String> sendShutDown;
    public final BlockingQueue<String> isShutDownDone;
    public final BlockingQueue<String> initFinished;
    public final BlockingQueue<String> killFinished;
    public final BlockingQueue<String> isECSShutdownDone;
    public final AtomicBoolean isECSShuttingDown;
    public final AtomicBoolean blockElection = new AtomicBoolean(false);
    public final AtomicBoolean isEcsAlive = new AtomicBoolean(false);
    public final AtomicBoolean isThereAnElection = new AtomicBoolean(false);

    private Socket ECSServerSocket;
    private ServerSocket ECSElectionServerSocket;
    private PrintWriter ECSServerOut;
    private BufferedReader ECSServerIn;
    private ScheduledFuture ECSPingFuture;

    public ECSServerHandler(Config config, ServerSenderSocket serverSenderSocket, AtomicBoolean isServerWorking,
                            AtomicBoolean isServerWriteLocked, BlockingQueue<String> sendShutDown,
                            BlockingQueue<String> isShutDownDone, BlockingQueue<String> initFinished,
                            BlockingQueue<String> killFinished, BlockingQueue<String> isECSShutdownDone, AtomicBoolean isECSShuttingDown) {
        this.config = config;
        this.serverSenderSocket = serverSenderSocket;
        this.isServerWorking = isServerWorking;
        this.isServerWriteLocked = isServerWriteLocked;
        this.sendShutDown = sendShutDown;
        this.isShutDownDone = isShutDownDone;
        this.initFinished = initFinished;
        this.killFinished = killFinished;
        this.isECSShutdownDone = isECSShutdownDone;
        this.isECSShuttingDown = isECSShuttingDown;
    }

    public void connectToECSHelper() {
        try {
            logger.info("Initializing connection with ECS Server: " + MetadataHandler.getEcsIP() + " " + config.ECSCommunicationPort);
            ECSServerSocket = new Socket(MetadataHandler.getEcsIP(), config.ECSCommunicationPort);
            ECSServerOut = new PrintWriter(new OutputStreamWriter(ECSServerSocket.getOutputStream(), Constants.TELNET_ENCODING));
            ECSServerIn = new BufferedReader(new InputStreamReader(ECSServerSocket.getInputStream(), Constants.TELNET_ENCODING));
            new Thread(new ECSConnectionHandler(isServerWorking, sendShutDown, isShutDownDone, isServerWriteLocked, config, serverSenderSocket, isEcsAlive, ECSServerOut, ECSServerIn, blockElection)).start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Triplet<Boolean, String, Boolean> ecsConnectionHelper(String currentIp) {
        boolean ecsFound = false;
        String ecsIp = currentIp;
        boolean isConnectionSuccessful = false;
        try {
            final Socket ecsServerCandidate = new Socket(currentIp, config.ElectionCommunicationPort);
            BufferedReader in = new BufferedReader(new InputStreamReader(ecsServerCandidate.getInputStream(), Constants.TELNET_ENCODING));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(ecsServerCandidate.getOutputStream(), Constants.TELNET_ENCODING));
            isConnectionSuccessful = true;
            String firstLine;
            out.write("get_ecs_info\r\n");
            out.flush();

            while ((firstLine = in.readLine()) != null) {
                // ecs oldugunu soyleyebilir
                if (firstLine.startsWith("ecs_me")) {
                    ecsIp = currentIp;
                    ecsFound = true;
                    logger.info("ecs found in: " + ecsIp);
                    break;
                }
                // ecs yerini soyle
                // ecs_ip ECS_IP
                else if (firstLine.startsWith("ecs_ip")) {
                    ecsIp = firstLine.split(" ")[1];

                    logger.info("Found the location of Ecs in another KV. Ecs is in: " + ecsIp);
                    break;
                }
                // ecs degil ama yerini bilmedigini soyleyebilir
                else if (firstLine.startsWith("ecs_no_info")) {
                    logger.info("No info about ECS with this kv-server: " + currentIp);
                    break;
                }
            }
            in.close();
            out.close();
            ecsServerCandidate.close();
        } catch (IOException e) {
            logger.info("The connection failed. There is no such KV-Server for this address: " + currentIp + " error: " + e.getMessage());
        }
        return new Triplet<>(ecsFound, ecsIp, isConnectionSuccessful);
    }

    public String findECSServer() {
        String ipConsistentPart = config.ipStart.substring(0, config.ipStart.lastIndexOf('.') + 1);
        int ipStartVariable = Integer.parseInt(config.ipStart.substring(config.ipStart.lastIndexOf('.') + 1));
        int ipRange = Integer.parseInt(config.ipRange);

        boolean isAnyKvServerFound = false;
        String ecsIp = config.listenaddr;

        // TODO exponential backoff should be added in case of an election when this node tries to enter
        do {
            for (int i = ipStartVariable; i < ipRange; i++) {
                String currentIp = ipConsistentPart + i;
                if (!currentIp.equals(ecsIp)) {
                    logger.info("Initializing connection with KV Server: " + ipConsistentPart + i + " " + 5000);
                    Triplet<Boolean, String, Boolean> result = ecsConnectionHelper(currentIp);

                    if (!isAnyKvServerFound && result.getValue2()) {
                        isAnyKvServerFound = true;
                    }

                    // if the ecsFound in the current ip, we simply return the ecs ip we found.
                    if (result.getValue0()) {
                        ecsIp = currentIp;
                        break;
                    } else if (!result.getValue1().equals(currentIp) && ecsConnectionHelper(result.getValue1()).getValue0()) {
                        //If the ecs is not found in the current ip but the KV-server we connect knows an ecs server location,
                        //we try to connect to the location and get
                        ecsIp = result.getValue1();
                        break;
                    }
                }
            }
        } while (isAnyKvServerFound && ecsIp.equals(config.listenaddr));

        return ecsIp;
    }

    public void startECSPingMessages() {
        ECSPingFuture = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            CompletableFuture.runAsync(() -> {
                String messageIP = MetadataHandler.getEcsIP();
                if (!blockElection.get() &&
                        !isThereAnElection.get() &&
                        !MetadataHandler.getEcsIP().equals(config.listenaddr) &&
                        messageIP.equals(MetadataHandler.getEcsIP())) {
                    try {
//                        logger.info(MetadataHandler.getEcsIP());
//                        logger.info(String.valueOf(blockElection.get()));
//                        logger.info("ping_heartbeat_ecs");
                        ECSServerOut.write("ping_heartbeat_ecs\r\n");
                        ECSServerOut.flush();
                        await().atMost(700, TimeUnit.MILLISECONDS).until(isEcsAlive::get);
                        isEcsAlive.set(false);
                    } catch (ConditionTimeoutException e) {
                        if (messageIP.equals(MetadataHandler.getEcsIP()) && !isThereAnElection.get()) {
                            logger.warning(MetadataHandler.getEcsIP() + " " + messageIP + " ECS is not responding!" + e.getMessage());
                            //this is where we detect ecs is down!
                            // TODO update ecsServer socket&stream
                            isThereAnElection.set(true);
                            startElection();
                        }
                    }
                }
            });
        }, 0, 1, TimeUnit.SECONDS);
    }

    public void electionMessageForward(String successorIP, String message) {
        try {
            logger.info("electionMessageForward " + successorIP);
            Socket successorSocket = new Socket(successorIP, config.ElectionCommunicationPort);
            PrintWriter out = new PrintWriter(new OutputStreamWriter(successorSocket.getOutputStream(), Constants.TELNET_ENCODING));
            out.write(message + "\r\n");
            out.flush();
            //successorSocket.close();
        } catch (UnknownHostException e) {
            logger.warning("Unknown host exception in election " + e);
        } catch (IOException io) {
            logger.warning("IOException in election  " + io);
        }
    }

    private void startElection() {
        String successor = MetadataHandler.getSuccessor(isThereAnElection);
        String currentHash = HashConverter.getMd5(MetadataHandler.getCurrentAddress());
        String successorIP = successor.split(":")[0];
        logger.info("Start election " + successorIP + " " + currentHash);
        getECSPingFuture().cancel(true);
        electionMessageForward(successorIP, "participant " + currentHash);
    }

    public Socket getECSServerSocket() {
        return ECSServerSocket;
    }

    public PrintWriter getECSServerOut() {
        return ECSServerOut;
    }

    public BufferedReader getECSServerIn() {
        return ECSServerIn;
    }

    public ScheduledFuture getECSPingFuture() {
        return ECSPingFuture;
    }

    public ServerSocket getECSElectionServerSocket() {
        return ECSElectionServerSocket;
    }

    public void setECSElectionServerSocket(ServerSocket ECSElectionServerSocket) {
        this.ECSElectionServerSocket = ECSElectionServerSocket;
    }
}
