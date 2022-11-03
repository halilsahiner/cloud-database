package de.tum.i13;

import de.tum.i13.server.kv.KVClientCommandProcessor;
import de.tum.i13.server.kv.KVMessage;
import de.tum.i13.server.kv.KVStoreInterface;
import de.tum.i13.server.kv.cache.DataCacheFIFOStore;
import de.tum.i13.server.kv.cache.DataCacheLFUStore;
import de.tum.i13.server.kv.cache.DataCacheLRUStore;
import de.tum.i13.server.kv.disk.DataDiskStore;
import de.tum.i13.shared.Config;
import de.tum.i13.shared.MetadataHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.LogManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test for cache and disk compatibility. The compatibility cases include the eviction of a pair from cache
 * and retrieval of that pair when it is asked from the disk according to the cache strategy it used.
 */
public class StorageTest {
    static Config config;
    static AtomicBoolean isServerWriteLocked = new AtomicBoolean(false);

    @AfterAll
    static void deleteAllTestFiles() {
        File directoryPath = new File(config.dataDir.toString());
        if (directoryPath.exists()) {
            File[] filesList = directoryPath.listFiles();
            if (filesList != null && filesList.length != 0)
                for (File f : filesList)
                    assertTrue(f.delete());
            assertTrue(directoryPath.delete());
        }
    }

    @BeforeEach
    public void StorageInitialize() {
        config = new Config();

        MetadataHandler.updateMetadata("00000000000000000000000000000000,00000000000000000000000000000000," + config.listenaddr + ":" + config.port + ";", config.listenaddr + ":" + config.port);
        config.dataDir = Paths.get("src/test/storage_test_resources");
        File directoryPath = new File(config.dataDir.toString());
        if (directoryPath.exists()) {
            File[] filesList = directoryPath.listFiles();
            if (filesList != null && filesList.length != 0)
                for (File f : filesList)
                    assertTrue(f.delete());
            assertTrue(directoryPath.delete());
        }
    }

    @Test
    public void StorageFIFOOverFlowTest() {

        KVStoreInterface diskStore = new DataDiskStore(config, LogManager.getLogManager().getLogger(""));
        int cacheSize = 5;
        KVStoreInterface cacheStore = new DataCacheFIFOStore(cacheSize);
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        KVClientCommandProcessor kvCommandProcessor = new KVClientCommandProcessor(lock, diskStore, cacheStore, "FIFO", isServerWriteLocked, config, null);
        // Fill the cache
        for (int i = 0; i < cacheSize + 1; i++) {
            kvCommandProcessor.process(String.format("put %d message %d", i, i));
        }
        String response = "get_success 0 message 0\r\n";
        assertEquals(kvCommandProcessor.process("get 0"), response);
        // Test whether the get process is carrying that pair to the cache as well
        assertEquals(cacheStore.get("0").getStatus(), KVMessage.StatusType.GET_SUCCESS);
    }

    @Test
    public void StorageLFUOverFlowTest() {
        KVStoreInterface diskStore = new DataDiskStore(config, LogManager.getLogManager().getLogger(""));
        int cacheSize = 5;
        KVStoreInterface cacheStore = new DataCacheLFUStore(cacheSize);
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        KVClientCommandProcessor kvCommandProcessor = new KVClientCommandProcessor(lock, diskStore, cacheStore, "LFU", isServerWriteLocked, config, null);
        // Fill the cache
        for (int i = 0; i < cacheSize; i++) {
            kvCommandProcessor.process(String.format("put %d message %d", i, i));
        }
        kvCommandProcessor.process("get 0");
        kvCommandProcessor.process(String.format("put %d message %d", 5, 5));
        String response = "get_success 1 message 1\r\n";
        assertEquals(kvCommandProcessor.process("get 1"), response);
        // Test whether the get process is carrying that pair to the cache as well
        assertEquals(cacheStore.get("1").getStatus(), KVMessage.StatusType.GET_SUCCESS);
    }

    @Test
    public void StorageLRUOverFlowTest() {
        KVStoreInterface diskStore = new DataDiskStore(config, LogManager.getLogManager().getLogger(""));
        int cacheSize = 5;
        KVStoreInterface cacheStore = new DataCacheLRUStore(cacheSize);
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        KVClientCommandProcessor kvCommandProcessor = new KVClientCommandProcessor(lock, diskStore, cacheStore, "LRU", isServerWriteLocked, config, null);
        // Fill the cache
        for (int i = 0; i < cacheSize; i++) {
            kvCommandProcessor.process(String.format("put %d message %d", i, i));
        }
        kvCommandProcessor.process("get 0");
        kvCommandProcessor.process(String.format("put %d message %d", 5, 5));
        String response = "get_success 1 message 1\r\n";
        assertEquals(kvCommandProcessor.process("get 1"), response);
        // Test whether the get process is carrying that pair to the cache as well
        assertEquals(cacheStore.get("1").getStatus(), KVMessage.StatusType.GET_SUCCESS);
    }

}
