package de.tum.i13;

import de.tum.i13.shared.Config;
import org.junit.jupiter.api.Test;

import static de.tum.i13.shared.Config.parseCommandlineArgs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestConfig {

    @Test
    public void testServerAddress() {
        String argSymbol = "-a";
        String address = "8.8.8.8";
        String[] args = new String[]{argSymbol + ' ' + address};
        Config cfg = parseCommandlineArgs(args);

        assertEquals(address, cfg.listenaddr.trim());
    }

    @Test
    public void testPortAddress() {
        String argSymbol = "-p";
        int port = 5554;
        String[] args = new String[]{argSymbol + port};
        Config cfg = parseCommandlineArgs(args);

        assertEquals(port, cfg.port);
    }

    @Test
    public void testCacheStrategy() {
        String argSymbol = "-s";
        String strategy = "FIFO";
        String[] args = new String[]{argSymbol + ' ' + strategy};
        Config cfg = parseCommandlineArgs(args);

        assertEquals(strategy, cfg.cacheStrategy.trim());
    }

    @Test
    public void testHelpArgument() {
        String argSymbol = "-h";
        String[] args = new String[]{argSymbol};
        Config cfg = parseCommandlineArgs(args);

        assertTrue(cfg.usagehelp);
    }


}
