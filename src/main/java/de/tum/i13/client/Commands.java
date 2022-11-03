package de.tum.i13.client;

/**
 * Commands enum class contains possible commands in application.
 * Commands object checks given string equals to one of the enum
 * values or is one of the enum values to determine whether it is a
 * valid command or not.
 */
public enum Commands {
    CONNECT("connect"),
    DISCONNECT("disconnect"),
    SEND("send"),
    LOGLEVEL("logLevel"),
    HELP("help"),
    QUIT("quit"),
    PUT("put"),
    DELETE("delete"),
    GET("get"),
    KEYRANGE("keyrange"),
    KEYRANGE_READ("keyrange_read");


    private final String command;

    Commands(String command) {
        this.command = command;
    }

    /**
     * Checks if the enum contains
     * given parameter value.
     *
     * @param command String value checked
     * @return true if enum contains command
     * false if enum does not contain command
     */
    public static boolean contains(String command) {
        for (Commands c : Commands.values()) {
            if (c.name().equalsIgnoreCase(command)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the command equals to
     * given parameter value.
     *
     * @param command String value checked
     * @return true if command equals
     * false if command does not equal
     */
    public boolean equalsValue(String command) {
        return this.name().equalsIgnoreCase(command);
    }
}
