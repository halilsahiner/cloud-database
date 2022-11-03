package de.tum.i13.shared;

import picocli.CommandLine;

import java.net.InetSocketAddress;
import java.nio.file.Path;

public class Config {
    @CommandLine.Option(names = "-p", description = "sets the port of the server", defaultValue = "5555")
    public int port;

    @CommandLine.Option(names = "-a", description = "which address the server should listen to", defaultValue = "127.0.0.1")
    public String listenaddr;

    @CommandLine.Option(names = "-b", description = "bootstrap broker where clients and other brokers connect first to retrieve configuration, port and ip, e.g., 192.168.1.1:5153", defaultValue = "clouddatabases.i13.in.tum.de:5153")
    public InetSocketAddress bootstrap;

    @CommandLine.Option(names = "-d", description = "Directory for files", defaultValue = "data/")
    public Path dataDir;

    @CommandLine.Option(names = "-l", description = "Logfile", defaultValue = "echo.log")
    public Path logfile;

    @CommandLine.Option(names = "-h", description = "Displays help", usageHelp = true, defaultValue = "false")
    public boolean usagehelp;

    @CommandLine.Option(names = "-c", description = "Size of the cache", defaultValue = "100")
    public int sizeOfCache;

    @CommandLine.Option(names = "-ll", description = "Log level type", defaultValue = "INFO")
    public String logLevel;

    @CommandLine.Option(names = "-s", description = "Cache replacement strategy", defaultValue = "FIFO")
    public String cacheStrategy;

    @CommandLine.Option(names = "-rs", description = "Starting IP for all servers in the system", defaultValue = "0.0.0.0")
    public String ipStart;

    @CommandLine.Option(names = "-re", description = "Range length of the IPs on the system", defaultValue = "255")
    public String ipRange;

    public final int ServerCommunicationPort = 3000;
    public final int ECSCommunicationPort = 4000;
    public final int ElectionCommunicationPort = 5000;


    public static Config parseCommandlineArgs(String[] args) {
        Config cfg = new Config();
        CommandLine.ParseResult parseResult = new CommandLine(cfg).registerConverter(InetSocketAddress.class, new InetSocketAddressTypeConverter()).parseArgs(args);

        if (!parseResult.errors().isEmpty()) {
            for (Exception ex : parseResult.errors()) {
                ex.printStackTrace();
            }

            CommandLine.usage(new Config(), System.out);
            System.exit(-1);
        }

        return cfg;
    }

    @Override
    public String toString() {
        return "Config{" +
                "port=" + port +
                ", listenaddr='" + listenaddr + '\'' +
                ", bootstrap=" + bootstrap +
                ", dataDir=" + dataDir +
                ", logfile=" + logfile +
                ", usagehelp=" + usagehelp +
                '}';
    }
}
