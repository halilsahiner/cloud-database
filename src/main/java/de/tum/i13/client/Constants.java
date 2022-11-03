package de.tum.i13.client;

public class Constants {
    public final static int MAX_MESSAGE_SIZE = 131072;
    public final static int MESSAGE_BUFFER_SIZE = 128;

    public final static String HELP_MESSAGE_CLIENT
            = "\n" +
            "This is a client application for connecting to a socket server with KV Storage functionality\n" +
            "The command list: \n" +
            "connect <address> <port> :	tries to establish a connection to a server with <address>+<port>\n" +
            "disconnect :				tries to disconnect from the connected server.\n" +
            "send <message> :			sends a text message to the echo server according to the communication protocol.\n" +
            "logLevel <LEVEL> :			set the level of log messages by setting the lowest level of logger to the given level\n" +
            "quit :						quits the application";

    public final static String HELP_MESSAGE_SERVER
            = "\n" +
            "This is a server application which has the capability of storing KV tuples with cache functionality\n" +
            "The command list: \n" +
            "-p  Sets the port of the server \n" +
            "-a  Which address the server should listen to, default is 127.0.0.1 \n" +
            "-b  Bootstrap broker (Used by the client as the first broker to connect to and all other brokers to bootstrap in the next Milestones) \n" +
            "-d  Directory for files (Put here the files you need to persist the data, the directory is created upfront and you can rely on that it exists) \n" +
            "-l  Relative path of the logfile, e.g., \"echo.log\". \n" +
            "-ll Loglevel, e.g., INFO, ALL, ... \n" +
            "-c  Size of the cache, e.g., 100 keys \n" +
            "-s  Cache displacement strategy, FIFO, LFU, LRU, \n" +
            "-h  Displays the help \n";
}
