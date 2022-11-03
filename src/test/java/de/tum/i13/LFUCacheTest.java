package de.tum.i13;

import de.tum.i13.server.kv.KVMessage;
import de.tum.i13.server.kv.KVStoreInterface;
import de.tum.i13.server.kv.cache.DataCacheLFUStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LFUCacheTest {
    @Test
    public void LFUCacheCreateFailTest() {
        int cacheSize = -1;
        assertThrows(IllegalArgumentException.class, () -> {
            KVStoreInterface kv = new DataCacheLFUStore(cacheSize);
        });
    }

    @Test
    public void LFUCachePutTest() {

        int cacheSize = 1;
        KVStoreInterface kv = new DataCacheLFUStore(cacheSize);

        assertSame(kv.put("1", String.format("message %d", 1)).getStatus(), KVMessage.StatusType.PUT_SUCCESS);
    }

    @Test
    public void LFUCacheGetTest() {
        int cacheSize = 1;
        KVStoreInterface kv = new DataCacheLFUStore(cacheSize);
        String key = "1";
        kv.put(key, String.format("message %d", 1));
        assertSame(kv.get(key).getStatus(), KVMessage.StatusType.GET_SUCCESS);
    }

    @Test
    public void LFUCacheMissTest() {

        int cacheSize = 3;
        KVStoreInterface kv = new DataCacheLFUStore(cacheSize);

        // Fill the cache
        for (int i = 0; i < cacheSize + 1; i++) {
            kv.put(Integer.toString(i), String.format("message %d", i));
        }

        // Check whether cache has the first pair inserted to cache
        assertSame(kv.get("0").getStatus(), KVMessage.StatusType.GET_ERROR);
    }

    @Test
    public void LFUCacheFrequentHitTest() {

        int cacheSize = 5;
        KVStoreInterface kv = new DataCacheLFUStore(cacheSize);

        // Fill the cache
        for (int i = 0; i < cacheSize; i++) {
            kv.put(Integer.toString(i), String.format("message %d", i));
        }
        kv.get("0");
        kv.get("0");
        kv.get("0");
        kv.get("1");
        kv.get("1");
        kv.get("2");
        kv.get("3");
        kv.get("4");
        kv.put("6", String.format("message %d", 6));

        // Check whether cache has the first pair inserted to cache
        assertSame(kv.get("3").getStatus(), KVMessage.StatusType.GET_SUCCESS);
    }

    @Test
    public void LFUCacheDeleteTest() {

        int cacheSize = 1;
        KVStoreInterface kv = new DataCacheLFUStore(cacheSize);
        String key = "1";
        kv.put(key, String.format("message %d", 1));

        assertSame(kv.delete(key).getStatus(), KVMessage.StatusType.DELETE_SUCCESS);
    }

    @Test
    public void LFUCacheGetDeletedTest() {

        int cacheSize = 1;
        KVStoreInterface kv = new DataCacheLFUStore(cacheSize);
        String key = "1";
        kv.put(key, String.format("message %d", 1));
        kv.delete(key);

        assertSame(kv.get(key).getStatus(), KVMessage.StatusType.GET_DELETED);
    }

}
