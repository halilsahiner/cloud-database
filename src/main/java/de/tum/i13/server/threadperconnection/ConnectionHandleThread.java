package de.tum.i13.server.threadperconnection;

import de.tum.i13.shared.CommandProcessor;
import de.tum.i13.shared.Constants;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class ConnectionHandleThread extends Thread {
    private static final Logger logger = Logger.getLogger(ConnectionHandleThread.class.getName());
    private final CommandProcessor cp;
    private final Socket connectionSocket;
    private final AtomicBoolean isServerWorking;

    public ConnectionHandleThread(CommandProcessor commandProcessor, Socket connectionSocket,
                                  AtomicBoolean isServerWorking) {
        this.cp = commandProcessor;
        this.connectionSocket = connectionSocket;
        this.isServerWorking = isServerWorking;
    }

    @Override
    public void run() {
        try {
            logger.info(connectionSocket.getRemoteSocketAddress().toString());
            BufferedReader in = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream(), Constants.TELNET_ENCODING));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(connectionSocket.getOutputStream(), Constants.TELNET_ENCODING));
            connectToClient(out);
            String firstLine;

//            logger.info("Reading the messages");
//            logger.info("Is server working: " + isServerWorking.get());
            while ((firstLine = in.readLine()) != null) {
                String res;
                if (!isServerWorking.get()) {
                    res = "server_stopped";
                } else {
                    res = cp.process(firstLine);
                }
                out.write(res);
                out.flush();
            }
            cp.connectionClosed(connectionSocket.getLocalAddress());
        } catch (UnsupportedEncodingException e) {
            logger.warning("UnsupportedEncodingException " + e);
        } catch (IOException e) {
            logger.warning("IOException " + e);
        } catch (NullPointerException e) {
            logger.warning("NullPointerException" + e);
        }

    }

    /**
     * If connection established, this
     * function send ack message to client that
     * indicates connection is successful.
     *
     * @param out PrinterWriter object for sending
     *            ack message to client.
     */
    private void connectToClient(PrintWriter out) {
        String res = cp.connectionAccepted((InetSocketAddress) connectionSocket.getLocalSocketAddress(), (InetSocketAddress) connectionSocket.getRemoteSocketAddress());
        out.write(res);
        out.flush();
    }
}
