package de.tum.i13.server.kv;

import de.tum.i13.ecs.ECSServerMain;
import de.tum.i13.shared.*;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class KVElectionCommandProcessor implements CommandProcessor {

    public static Logger logger = Logger.getLogger(KVElectionCommandProcessor.class.getName());
    private final Config config;
    private final ECSServerHandler ecsServerHandler;


    public KVElectionCommandProcessor(Config config, ECSServerHandler ecsServerHandler) {
        this.config = config;
        this.ecsServerHandler = ecsServerHandler;
    }

    /**
     * @param command command comes from other servers.
     * @return the message that is created after the command is processed.
     */
    @Override
    public String process(String command) {
        KVMessage message = new Message(null, null, KVMessage.StatusType.UNRECOGNIZED_COMMAND);

        logger.info(String.valueOf(ecsServerHandler.blockElection.get()));
        if (!ecsServerHandler.blockElection.get()) {
            if (command.equalsIgnoreCase("get_ecs_info")) {
                if (MetadataHandler.getEcsIP().equals(config.listenaddr)) {
                    message = new Message(null, MetadataHandler.getEcsIP(), KVMessage.StatusType.ECS_ME);
                } else if (ecsServerHandler.isThereAnElection.get()) {
                    message = new Message(null, null, KVMessage.StatusType.ECS_NO_INFO);
                } else {
                    message = new Message(null, MetadataHandler.getEcsIP(), KVMessage.StatusType.ECS_IP);
                }
            } else if (command.startsWith("participant")) {
                ecsServerHandler.getECSPingFuture().cancel(true);
                String receivedHashValue = command.split(" ")[1];
                String successor = MetadataHandler.getSuccessor(ecsServerHandler.isThereAnElection);
                String successorIP = successor.split(":")[0];
                String currentHashValue = HashConverter.getMd5(MetadataHandler.getCurrentAddress());
                /*logs gonna log*/
                logger.info("COMING COMMAND: " + command);
                logger.info("RECEIVED: " + receivedHashValue);
                logger.info("CURRENT: " + currentHashValue);
                logger.info("IS THERE AN ELECTION: " + ecsServerHandler.isThereAnElection.get());

                if (receivedHashValue.compareTo(currentHashValue) < 0) {
                    if (!ecsServerHandler.isThereAnElection.get()) {
                        message = new Message(null, currentHashValue, KVMessage.StatusType.PARTICIPANT);
                        ecsServerHandler.isThereAnElection.set(true);
                    }
                } else if (receivedHashValue.equals(currentHashValue)) {
                    // now we are elected
                    if(config.listenaddr.equals(MetadataHandler.getEcsIP())) {
                        return message.toString();
                    }
                    MetadataHandler.setEcsIP(config.listenaddr);
                    // ecs server should be started since we are elected
                    try {
                        ecsServerHandler.getECSServerSocket().close();
                        ServerSocket serverSocket = new ServerSocket();
                        serverSocket.bind(new InetSocketAddress(config.listenaddr, config.ECSCommunicationPort));
                        logger.info("ECS Socket bound to address " + config.listenaddr + " " + config.ECSCommunicationPort);
                        List<String> previousMetadataKeys = new ArrayList<>(MetadataHandler.getMetadata().keySet());
                        new Thread(new ECSServerMain(ecsServerHandler.initFinished, ecsServerHandler.killFinished,
                                ecsServerHandler.isECSShutdownDone, ecsServerHandler.isECSShuttingDown, config,
                                serverSocket, previousMetadataKeys)).start();
                        ecsServerHandler.connectToECSHelper();
                        ecsServerHandler.isThereAnElection.set(true);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    message = new Message(null, config.listenaddr, KVMessage.StatusType.ELECTED);
                } else {
                    ecsServerHandler.isThereAnElection.set(true);
                    message = new Message(null, receivedHashValue, KVMessage.StatusType.PARTICIPANT);
                }
                ecsServerHandler.electionMessageForward(successorIP, message.toString());
            } else if (command.startsWith("elected")) {
                String receivedECSIP = command.split(" ")[1];
                String successor = MetadataHandler.getSuccessor(ecsServerHandler.isThereAnElection);
                String successorIP = successor.split(":")[0];

                if (receivedECSIP.equals(config.listenaddr)) {
                    logger.info("IAM THE ECS AND MY IP:" + config.listenaddr);
                    try {
                        Socket successorSocket = new Socket(config.listenaddr, config.ECSCommunicationPort);
                        PrintWriter out = new PrintWriter(new OutputStreamWriter(successorSocket.getOutputStream(), Constants.TELNET_ENCODING));
                        out.write("remove_block_ecs"+"\r\n");
                        out.flush();
                        successorSocket.close();
                    } catch (UnknownHostException e) {
                        logger.warning("Unknown host exception in election "+e);
                    } catch (IOException io) {
                        logger.warning("IOException in election  "+io);
                    }
                } else {
                    logger.info("NOT ECS, forwarding elected messages");
                    MetadataHandler.setEcsIP(receivedECSIP);
                    ecsServerHandler.connectToECSHelper();
                    message = new Message(null, receivedECSIP, KVMessage.StatusType.ELECTED);
                    ecsServerHandler.electionMessageForward(successorIP, message.toString());
                    ecsServerHandler.isThereAnElection.set(false);
                    ecsServerHandler.blockElection.set(true);
                    ecsServerHandler.startECSPingMessages();
                    logger.info("IS THERE AN ELECTION: " + ecsServerHandler.isThereAnElection.get());
                }
            }
        }

        logger.info("Returns message : " + message);
        return message.toString();
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
