package de.tum.i13.client;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import static de.tum.i13.client.Constants.MAX_MESSAGE_SIZE;

/**
 * Client object creates connection with echo server
 * and handles communication between this server.
 * It sends messages to server and receives echo messages.
 * Prints echo messages when they received.
 * Disconnects with echo server when communication is over.
 */
public class ActiveConnection {
    private final static Logger LOGGER = Logger.getLogger(CommandLineInterface.class.getName());
    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private boolean isConnected = false;

    /**
     * Tries to establish a
     * TCP connection to the echo server based on
     * the given server address and the port
     * number of the echo service. Once the connection is established, the
     * echo server will reply with a confirmation message.
     *
     * @return true if connection is successful.
     * false if connection failed.
     */
    public boolean connect(String address, int port) {
        if (socket != null && isConnected) {
            LOGGER.info("Closing current connection " + socket.getInetAddress().getHostName() + " " + socket.getPort());
            disconnect();
        }

        try {
            LOGGER.info("Connecting to server");
            socket = new Socket(address, port);
        } catch (UnknownHostException e) {
            LOGGER.info("The connection address or port number is wrong.");
            LOGGER.info("Address : " + address + " port: " + port);
            LOGGER.severe(e.getMessage());
            return false;
        } catch (IOException e) {
            LOGGER.warning("Connection failed.");
            LOGGER.severe(e.getMessage());
            return false;
        }

        try {
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
        } catch (IOException e) {
            LOGGER.warning("Input or output stream creation failed.");
            LOGGER.severe(e.getMessage());
            return false;
        }
        isConnected = true;
        LOGGER.info("Connection established.");
        System.out.println(receiveMessage());
        return true;
    }

    /**
     * Tries to disconnect from the connected server.
     *
     * @return true if disconnection process is successful.
     * false if disconnection failed.
     */
    public boolean disconnect() {
        LOGGER.finest("Disconnection attempt begins.");
        try {
            inputStream.close();
            outputStream.close();
            socket.close();
        } catch (IOException e) {
            LOGGER.warning("Closing socket is failed.");
            LOGGER.severe(e.getMessage());
            return false;
        }
        isConnected = false;
        LOGGER.info("Client is disconnected from server successfully.");
        System.out.println("Connection terminated: " + socket.getInetAddress() + " / " + socket.getPort());
        return true;
    }

    /**
     * Sends a text message to the echo server
     * according to the communication protocol.
     *
     * @param message Sequence of ISO-8859-1 coded
     *                characters that correspond to the
     *                application-specific protocol.
     * @return true if message is sent.
     * false if message sending failed.
     */
    public boolean sendMessage(String message) {
        if (outputStream == null) {
            LOGGER.info("There is no connection to send a message");
            System.out.println("There is no connection to send a message");
            return false;
        }

        message = message + "\r\n";
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);

        LOGGER.info("Send message attempt begins.");
        try {
            if (bytes.length > MAX_MESSAGE_SIZE) {
                System.out.println("The message size exceeds the maximum length allowed by the server.");
                LOGGER.warning("Message size exceeds 128KB. " + bytes.length);
                return false;
            } else {
                send(bytes);
                LOGGER.finest("Message sent to server successfully.");
            }
        } catch (IOException e) {
            LOGGER.warning("Sending message is failed.");
            LOGGER.severe(e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Writes len bytes from the specified byte array
     * starting at offset off to this output stream.
     *
     * @param bytes byte array received from server.
     * @throws IOException if an I/O error occurs.
     */
    private void send(byte[] bytes) throws IOException {
        outputStream.write(bytes, 0, bytes.length);
        outputStream.flush();
    }

    /**
     * Reads the output stream of the connected client
     * according to the communication protocol.
     * Once the echo server receives the message
     * it will send back the same message to the client.
     *
     * @return the received message
     */
    public String receiveMessage() {
        String receivedMessage = null;

        try {
            LOGGER.fine("Reading the input stream of the server.");
            ByteArrayOutputStream buffer = receive();
            if (buffer.size() == 0) {
                LOGGER.warning("Input stream is empty.");
            } else {
                receivedMessage = buffer.toString();
                buffer.flush();
            }
        } catch (IOException e) {
            LOGGER.warning("Receiving message is failed.");
            LOGGER.severe(e.getMessage());
        }

        if (receivedMessage != null) {
            LOGGER.fine("Message echoed back from server successfully.");
        } else {
            return "";
        }

        return receivedMessage;
    }

    /**
     * Reads up to len bytes of data from
     * the input stream into an array of bytes.
     * Writes bytes to a buffer. Ensures that
     * buffer filled with complete echo message.
     *
     * @return buffer filled up with receiving message
     * @throws IOException if an I/O error occurs.
     */
    private ByteArrayOutputStream receive() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(inputStream, de.tum.i13.shared.Constants.TELNET_ENCODING));
            String firstLine;
            while ((firstLine = in.readLine()) != null) {
                buffer.write(firstLine.getBytes(de.tum.i13.shared.Constants.TELNET_ENCODING));
                break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buffer;
    }

    /**
     * Returns connection status of socket
     *
     * @return true if it is connected
     * false if it is not connected
     */
    public boolean isConnected() {
        return isConnected;
    }
}
