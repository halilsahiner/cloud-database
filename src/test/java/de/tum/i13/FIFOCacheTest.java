package de.tum.i13;

import de.tum.i13.server.kv.KVMessage;
import de.tum.i13.server.kv.KVStoreInterface;
import de.tum.i13.server.kv.cache.DataCacheFIFOStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class FIFOCacheTest {

    @Test
    public void FIFOCacheCreateFailTest() {

        int cacheSize = -1;
        assertThrows(IllegalArgumentException.class, () -> {
            KVStoreInterface kv = new DataCacheFIFOStore(cacheSize);
        });
    }

    @Test
    public void FIFOCachePutTest() {

        int cacheSize = 1;
        KVStoreInterface kv = new DataCacheFIFOStore(cacheSize);

        assertSame(kv.put("1", String.format("message %d", 1)).getStatus(), KVMessage.StatusType.PUT_SUCCESS);
    }

    @Test
    public void FIFOCacheGetTest() {

        int cacheSize = 1;
        KVStoreInterface kv = new DataCacheFIFOStore(cacheSize);
        String key = "1";
        kv.put(key, String.format("message %d", 1));
        assertSame(kv.get(key).getStatus(), KVMessage.StatusType.GET_SUCCESS);
    }

    @Test
    public void FIFOCacheMissTest() {

        int cacheSize = 3;
        KVStoreInterface kv = new DataCacheFIFOStore(cacheSize);

        // Fill the cache
        for (int i = 0; i < cacheSize + 1; i++) {
            kv.put(Integer.toString(i), String.format("message %d", i));
        }

        // Check whether cache has the first pair inserted to cache
        assertSame(kv.get("0").getStatus(), KVMessage.StatusType.GET_ERROR);
    }

    @Test
    public void FIFOCacheDeleteTest() {

        int cacheSize = 1;
        KVStoreInterface kv = new DataCacheFIFOStore(cacheSize);
        String key = "1";
        kv.put(key, String.format("message %d", 1));

        assertSame(kv.delete(key).getStatus(), KVMessage.StatusType.DELETE_SUCCESS);
    }

    @Test
    public void FIFOCacheGetDeletedTest() {

        int cacheSize = 1;
        KVStoreInterface kv = new DataCacheFIFOStore(cacheSize);
        String key = "1";
        kv.put(key, String.format("message %d", 1));
        kv.delete(key);

        assertSame(kv.get(key).getStatus(), KVMessage.StatusType.GET_DELETED);
    }
}
