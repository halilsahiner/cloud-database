package de.tum.i13.server.kv.cache;

import de.tum.i13.server.kv.KVMessage;
import de.tum.i13.server.kv.KVStoreInterface;
import de.tum.i13.server.kv.Message;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

/**
 * A cache implementation with a FIFO strategy with the put(add & update), get and delete functionality
 */
public class DataCacheFIFOStore implements KVStoreInterface {

    private final Queue<String> FIFOTracker;
    private final HashMap<String, String> FIFOCache;
    private final int sizeOfCache;
    private int currentSizeOfCache = 0;

    public DataCacheFIFOStore(int sizeOfCache) {
        if (sizeOfCache < 0) {
            throw new IllegalArgumentException("Illegal cache capacity: " + sizeOfCache);
        }
        this.sizeOfCache = sizeOfCache;
        this.FIFOTracker = new LinkedList<>();
        this.FIFOCache = new HashMap<>();
    }

    @Override
    public KVMessage put(String key, String value) {
        boolean isUpdated;
        if (!FIFOCache.containsKey(key)) {
            isUpdated = false;
            // if the cache is full, remove the pair that is first inserted
            if (currentSizeOfCache == sizeOfCache) {
                FIFOCache.remove(FIFOTracker.remove());
            } else {
                // if size is not full, the current cache size should increase
                currentSizeOfCache++;
            }
            // new key should be added to fifo tracker
            FIFOTracker.add(key);
        } else {
            isUpdated = FIFOCache.get(key) != null;
        }
        // the cache value is updated
        FIFOCache.put(key, value);

        return new Message(key, value, isUpdated ? KVMessage.StatusType.PUT_UPDATE : KVMessage.StatusType.PUT_SUCCESS);
    }

    @Override
    public KVMessage get(String key) {
        if (!FIFOCache.containsKey(key)) {
            return new Message(key, null, KVMessage.StatusType.GET_ERROR);
        }
        String value = FIFOCache.get(key);

        // if the key is found, but it is recently deleted by client, the cache returns a deleted status
        return new Message(key, value, value == null ? KVMessage.StatusType.GET_DELETED : KVMessage.StatusType.GET_SUCCESS);
    }

    @Override
    public KVMessage delete(String key) {
        if (!FIFOCache.containsKey(key) || FIFOCache.get(key) == null) {
            return new Message(key, null, KVMessage.StatusType.DELETE_ERROR);
        }
        // The null value is a tombstone for the cache to give a fast get response of recently deleted values
        FIFOCache.put(key, null);

        return new Message(key, null, KVMessage.StatusType.DELETE_SUCCESS);
    }
}
