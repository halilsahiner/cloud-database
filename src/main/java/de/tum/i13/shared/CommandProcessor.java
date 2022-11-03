package de.tum.i13.shared;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public interface CommandProcessor {

    /**
     * Processes commands come from client side.
     * Calls get or put functions considering this
     * commands.
     *
     * @param command command comes from client side.(put or get)
     * @return an informative string for client.
     */
    String process(String command);

    /**
     * If a connection established with client, this
     * function is called. It sends an ack-knowledge
     * message to client.
     *
     * @param address       the address of the endpoint this socket is bound to.
     * @param remoteAddress the address of the endpoint this socket is connected to.
     * @return ack-knowledge message for client.
     */
    String connectionAccepted(InetSocketAddress address, InetSocketAddress remoteAddress);

    /**
     * If connection closed with client, this function
     * is called. It simply set logs about disconnection.
     *
     * @param address the local address to which the socket is bound.
     */
    void connectionClosed(InetAddress address);
}
