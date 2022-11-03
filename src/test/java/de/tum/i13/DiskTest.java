package de.tum.i13;

import de.tum.i13.server.kv.KVMessage;
import de.tum.i13.server.kv.KVStoreInterface;
import de.tum.i13.server.kv.disk.DataDiskStore;
import de.tum.i13.shared.Config;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Paths;
import java.util.logging.LogManager;

import static org.junit.jupiter.api.Assertions.assertSame;

public class DiskTest {

    static Config config;

    @AfterAll
    static void deleteAllTestFiles() {
        File directoryPath = new File(config.dataDir.toString());
        if (directoryPath.exists()) {
            File[] filesList = directoryPath.listFiles();
            if (filesList != null && filesList.length != 0)
                for (File f : filesList)
                    f.delete();
            directoryPath.delete();
        }
    }

    @BeforeEach
    public void StorageInitialize() {
        config = new Config();
        config.dataDir = Paths.get("src/test/storage_test_resources");
        File directoryPath = new File(config.dataDir.toString());
        if (directoryPath.exists()) {
            File[] filesList = directoryPath.listFiles();
            if (filesList != null && filesList.length != 0)
                for (File f : filesList)
                    f.delete();
            directoryPath.delete();
        }
    }

    @Test
    public void DiskPutTest() {
        KVStoreInterface diskStore = new DataDiskStore(config, LogManager.getLogManager().getLogger(""));

        assertSame(diskStore.put("1", String.format("message %d", 1)).getStatus(), KVMessage.StatusType.PUT_SUCCESS);
    }

    @Test
    public void DiskGetTest() {
        KVStoreInterface diskStore = new DataDiskStore(config, LogManager.getLogManager().getLogger(""));
        String key = "1";
        diskStore.put(key, String.format("message %d", 1));
        assertSame(diskStore.get(key).getStatus(), KVMessage.StatusType.GET_SUCCESS);
    }

    @Test
    public void DiskDeleteTest() {
        KVStoreInterface diskStore = new DataDiskStore(config, LogManager.getLogManager().getLogger(""));
        String key = "1";
        diskStore.put(key, String.format("message %d", 1));

        assertSame(diskStore.delete(key).getStatus(), KVMessage.StatusType.DELETE_SUCCESS);
    }

    @Test
    public void DiskGetDeletedTest() {

        KVStoreInterface diskStore = new DataDiskStore(config, LogManager.getLogManager().getLogger(""));
        String key = "1";
        diskStore.put(key, String.format("message %d", 1));
        diskStore.delete(key);

        assertSame(diskStore.get(key).getStatus(), KVMessage.StatusType.GET_DELETED);
    }
}
