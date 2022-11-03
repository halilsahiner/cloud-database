package de.tum.i13.client;

import de.tum.i13.shared.HashConverter;
import de.tum.i13.shared.MetadataHelper;
import org.javatuples.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static de.tum.i13.client.Constants.HELP_MESSAGE_CLIENT;
import static de.tum.i13.shared.LogSetup.setupLogging;

public class CommandLineInterface {
    private final static Logger LOGGER = Logger.getLogger(CommandLineInterface.class.getName());
    private static final int maxBackOff = 1000;
    private static final int maxKeySearch = 5;
    static ActiveConnection client = new ActiveConnection();
    private static HashMap<String, Pair<String, String>> metadata = new HashMap<>();


    public void run() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        Path logDirectory = Paths.get("src", "/client.log");

        setupLogging(logDirectory);

        while (true) {
            System.out.print("EchoClient>");
            String input = reader.readLine();
            String[] tokens = parseTokens(input);
            String firstWord = tokens[0];

            if (input.equals("")) {
                continue;
            } else if (!Commands.contains(firstWord)) {
                System.out.println(HELP_MESSAGE_CLIENT);
            } else if (Commands.CONNECT.equalsValue(firstWord) && tokens.length == 3 && tokens[2].matches("[0-9]+")) {
                connect(tokens);
            } else if (Commands.SEND.equalsValue(firstWord)) {
                sendTextMessage(input);
            } else if (Commands.DISCONNECT.equalsValue(firstWord) && tokens.length == 1) {
                disconnect();
            } else if (Commands.PUT.equalsValue(firstWord) && tokens.length >= 2) {
                kvOperation(input);
            } else if (Commands.DELETE.equalsValue(firstWord) && tokens.length == 2) {
                kvOperation(input);
            } else if (Commands.GET.equalsValue(firstWord) && tokens.length == 2) {
                kvOperation(input);
            } else if (Commands.KEYRANGE.equalsValue(firstWord)) {
                System.out.println(keyRangeHelper());
            } else if (Commands.KEYRANGE_READ.equalsValue(firstWord)) {
                System.out.println(keyRangeReadHelper());
            } else if (Commands.HELP.equalsValue(firstWord) && tokens.length == 1) {
                System.out.println(HELP_MESSAGE_CLIENT);
            } else if (Commands.LOGLEVEL.equalsValue(firstWord) && tokens.length == 1) {
                changeLogLevel(tokens[0], LOGGER);
            } else if (Commands.QUIT.equalsValue(firstWord) && tokens.length == 1) {
                if (client.isConnected()) {
                    client.disconnect();
                }
                System.out.println("Client application is closing.");
                return;
            } else {
                System.out.println(HELP_MESSAGE_CLIENT);
            }
        }
    }

    /**
     * Set the log level specifying which message
     * levels will be logged by this logger.
     *
     * @param level Level of the logger.
     */
    public void changeLogLevel(String level, Logger logger) {
        logger.finest("Log level changing attempt begins.");
        try {
            Level parsedLevel = Level.parse(level);
            logger.setLevel(parsedLevel);
            logger.info("Log level has been set to desired level.");
        } catch (IllegalArgumentException e) {
            logger.warning("Given \"" + level + "\" log level doesn't exist.");
            System.out.println("Given \"" + level + "\" log level doesn't exist.");
        }
    }

    public String[] parseTokens(String input) {
        return input.trim().split("\\s+");
    }

    public boolean connect(String[] tokens) {
        String address = tokens[1];
        int port = Integer.parseInt(tokens[2]);

        boolean isConnected = client.connect(address, port);
        if (!isConnected) {
            System.out.println("Connection failed.");
        }
        return isConnected;
    }

    public String send(String message) {
        String response = null;
        if (client.isConnected()) {
            client.sendMessage(message);
            response = client.receiveMessage();
        } else {
            System.out.println("There is no connection to send message.");
        }
        return response;
    }

    public boolean disconnect() {
        boolean isDisconnect;
        if (client.isConnected()) {
            isDisconnect = client.disconnect();
            if (!isDisconnect) {
                System.out.println("Disconnection failed.");
            }
        } else {
            isDisconnect = true;
            System.out.println("No connection is found.");
        }
        return isDisconnect;
    }

    public void sendTextMessage(String input) {
        send(input.substring(input.indexOf(" ") + 1));
    }


    public String kvOperation(String input) {
        String replyMessage = "";
        replyMessage = send(input);

        String[] commands = input.trim().split(" ");
        String operation = commands[0];
        String key = commands[1];
        String value = commands.length > 2 ? commands[2] : "";
        LOGGER.info("Input command: " + operation + " key: " + key);
        int attempts = 1;
        while (replyMessage.equals("server_stopped")) {
            LOGGER.info("Server is not initialized. Retrying operation....");
            try {
                TimeUnit.SECONDS.sleep(getWaitTime(attempts)); // sleep for that specific time
            } catch (InterruptedException e) {
                LOGGER.warning("Error occurred during exponential backoff :" + e);
            }
            attempts++;
            replyMessage = send(input);
        }

        if (replyMessage.equals("server_write_lock")) {
            LOGGER.warning("Server is currently doing an write operation. Try again later. ? ");
            return "server_write_lock";
        }

        attempts = 0;
        while (replyMessage.equals("server_not_responsible")) {
            String rawMetadata;
            if (operation.equals("get")) {
                rawMetadata = send("keyrange_read");
            } else {
                rawMetadata = send("keyrange");
            }
            LOGGER.info("new keyrange is: " + rawMetadata);
            metadata = MetadataHelper.parseMetadata(rawMetadata.split("\\s+")[1]);
            disconnect();
            String[] targetServer = getTargetServer(key);
            if (targetServer.length == 0) {
                continue;
            }
            connect(targetServer);
            replyMessage = send(input);
            attempts++;
            if (attempts > maxKeySearch) {
                LOGGER.warning("Couldn't find responsible server for key: " + key);
                return "Exceeded trial limit for server search";
            }
        }

        if (replyMessage.startsWith(operation + "_success") || replyMessage.startsWith(operation + "_update")) {
            LOGGER.info("Operation completed successfully");
        } else {
            LOGGER.warning("Couldn't get the success message back.: " + replyMessage);
        }
        System.out.println(replyMessage);
        return replyMessage;

    }

    public String keyRangeHelper() {
        return send("keyrange");
    }

    public String keyRangeReadHelper() {
        return send("keyrange_read");
    }

    public int getWaitTime(int attempts) {
        Random random = new Random();
        double pow = Math.pow(2, attempts);
        int rand = random.nextInt(maxBackOff);
        return (int) Math.min(pow + rand, maxBackOff);
    }

    public String[] getTargetServer(String key) {
        for (HashMap.Entry<String, Pair<String, String>> mapEntry : metadata.entrySet()) {
            if (HashConverter.isInKeyRange(mapEntry.getValue(), key)) {
                String address = mapEntry.getKey();
                LOGGER.info("Connected server : " + mapEntry + " for key" + key);
                return new String[]{"connect", MetadataHelper.getHostAddress(address), MetadataHelper.getHostPort(address)};
            }
        }

        LOGGER.warning("Couldn't find target server in metadata.");
        return new String[]{};
    }
}
