package de.tum.i13;

import de.tum.i13.server.kv.KVMessage;
import de.tum.i13.server.kv.KVStoreInterface;
import de.tum.i13.server.kv.cache.DataCacheLRUStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LRUCacheTest {
    @Test
    public void LRUCacheCreateFailTest() {
        int cacheSize = -1;
        assertThrows(IllegalArgumentException.class, () -> {
            KVStoreInterface kv = new DataCacheLRUStore(cacheSize);
        });
    }

    @Test
    public void LRUCachePutTest() {

        int cacheSize = 1;
        KVStoreInterface kv = new DataCacheLRUStore(cacheSize);

        assertSame(kv.put("1", String.format("message %d", 1)).getStatus(), KVMessage.StatusType.PUT_SUCCESS);
    }

    @Test
    public void LRUCacheGetTest() {
        int cacheSize = 1;
        KVStoreInterface kv = new DataCacheLRUStore(cacheSize);
        String key = "1";
        kv.put(key, String.format("message %d", 1));
        assertSame(kv.get(key).getStatus(), KVMessage.StatusType.GET_SUCCESS);
    }

    @Test
    public void LRUCacheMissTest() {

        int cacheSize = 3;
        KVStoreInterface kv = new DataCacheLRUStore(cacheSize);

        // Fill the cache
        for (int i = 0; i < cacheSize + 1; i++) {
            kv.put(Integer.toString(i), String.format("message %d", i));
        }

        // Check whether cache has the first pair inserted to cache
        assertSame(kv.get("0").getStatus(), KVMessage.StatusType.GET_ERROR);
    }

    @Test
    public void LRUCacheFrequentHitTest() {

        int cacheSize = 5;
        KVStoreInterface kv = new DataCacheLRUStore(cacheSize);

        // Fill the cache
        for (int i = 0; i < cacheSize; i++) {
            kv.put(Integer.toString(i), String.format("message %d", i));
        }
        kv.get("0");
        kv.put("6", String.format("message %d", 3));

        // Check whether cache has the first pair inserted to cache
        assertSame(kv.get("0").getStatus(), KVMessage.StatusType.GET_SUCCESS);
    }

    @Test
    public void LRUCacheDeleteTest() {

        int cacheSize = 1;
        KVStoreInterface kv = new DataCacheLRUStore(cacheSize);
        String key = "1";
        kv.put(key, String.format("message %d", 1));

        assertSame(kv.delete(key).getStatus(), KVMessage.StatusType.DELETE_SUCCESS);
    }

    @Test
    public void LRUCacheGetDeletedTest() {

        int cacheSize = 1;
        KVStoreInterface kv = new DataCacheLRUStore(cacheSize);
        String key = "1";
        kv.put(key, String.format("message %d", 1));
        kv.delete(key);

        assertSame(kv.get(key).getStatus(), KVMessage.StatusType.GET_DELETED);
    }

}
