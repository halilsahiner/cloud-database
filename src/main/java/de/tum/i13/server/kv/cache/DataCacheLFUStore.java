package de.tum.i13.server.kv.cache;

import de.tum.i13.server.kv.KVMessage;
import de.tum.i13.server.kv.KVStoreInterface;
import de.tum.i13.server.kv.Message;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.logging.Logger;

/**
 * A cache implementation with an LFU strategy with the put(add & update), get and delete functionality
 */
public class DataCacheLFUStore implements KVStoreInterface {
    public static Logger logger = Logger.getLogger(DataCacheLFUStore.class.getName());
    private final HashMap<String, String> LFUCache;//cache K and V
    private final HashMap<String, Integer> FrequencyMapper;//K and counters
    private final HashMap<Integer, LinkedHashSet<String>> FrequencyTracker;//Counter and item list
    private final int sizeOfCache;
    private int currentSizeOfCache = 0;
    private int minimumFrequency = -1;


    public DataCacheLFUStore(int sizeOfCache) {
        if (sizeOfCache < 0) {
            throw new IllegalArgumentException("Illegal cache capacity: " + sizeOfCache);
        }
        this.sizeOfCache = sizeOfCache;
        LFUCache = new HashMap<>();
        FrequencyMapper = new HashMap<>();
        FrequencyTracker = new HashMap<>();
    }

    @Override
    public KVMessage put(String key, String value) {
        boolean isUpdated;
        if (!LFUCache.containsKey(key)) {
            isUpdated = false;
            addPairTrackers(key);
        } else {
            isUpdated = LFUCache.get(key) != null;
            updatePairTrackers(key);
        }
        // the cache value is updated
        LFUCache.put(key, value);

        return new Message(key, value, isUpdated ? KVMessage.StatusType.PUT_UPDATE : KVMessage.StatusType.PUT_SUCCESS);
    }

    @Override
    public KVMessage get(String key) {
        if (!LFUCache.containsKey(key)) {
            return new Message(key, null, KVMessage.StatusType.GET_ERROR);
        } else {
            String value = LFUCache.get(key);
            updatePairTrackers(key);

            // if the key is found, but it is recently deleted by client, the cache returns a deleted status
            return new Message(key, value, value == null ? KVMessage.StatusType.GET_DELETED : KVMessage.StatusType.GET_SUCCESS);
        }
    }

    @Override
    public KVMessage delete(String key) {
        if (!LFUCache.containsKey(key) || LFUCache.get(key) == null) {
            return new Message(key, null, KVMessage.StatusType.DELETE_ERROR);
        }
        // The null value is a tombstone for the cache to give a fast get response of recently deleted values
        LFUCache.put(key, null);
        updatePairTrackers(key);

        return new Message(key, null, KVMessage.StatusType.DELETE_SUCCESS);
    }

    private void evictPair() {
        try {
            String key = FrequencyTracker.get(minimumFrequency).iterator().next();
            FrequencyTracker.get(minimumFrequency).remove(key);
            if (FrequencyTracker.get(minimumFrequency).isEmpty()) {
                FrequencyTracker.remove(minimumFrequency);
                minimumFrequency = Integer.MAX_VALUE;
                for (int frequency : FrequencyTracker.keySet()) {
                    if (frequency < minimumFrequency) {
                        minimumFrequency = frequency;
                    }
                }
            }
            FrequencyMapper.remove(key);
            LFUCache.remove(key);
        } catch (NullPointerException e) {
            logger.info("Min Freq: " + minimumFrequency);
            logger.info("Cache : " + LFUCache);
            logger.info("FrequencyMapper : " + FrequencyMapper);
            logger.info("FrequencyTracker : " + FrequencyTracker);
            throw e;
        }
    }

    private void updatePairTrackers(String key) {
        int currentFrequency = FrequencyMapper.get(key);
        int updatedFrequency = currentFrequency + 1;

        FrequencyMapper.put(key, updatedFrequency);

        // remove node from previous frequency group
        FrequencyTracker.get(currentFrequency).remove(key);
        if (FrequencyTracker.get(currentFrequency).isEmpty()) {
            if (currentFrequency == minimumFrequency) {
                minimumFrequency = updatedFrequency;
            }
            FrequencyTracker.remove(currentFrequency);
        }
        // Add the key to existing frequency group
        if (FrequencyTracker.containsKey(updatedFrequency)) {
            FrequencyTracker.get(updatedFrequency).add(key);
        } else {
            // if there is  no frequency group, create new one
            LinkedHashSet<String> newLinkedHashSet = new LinkedHashSet<>();
            newLinkedHashSet.add(key);
            FrequencyTracker.put(updatedFrequency, newLinkedHashSet);
        }
    }

    private void addPairTrackers(String key) {
        // if the cache is full, remove the pair that is first inserted
        if (currentSizeOfCache == sizeOfCache) {
            evictPair();
        } else {
            // if size is not full, the current cache size should increase
            currentSizeOfCache++;
        }
        // new key should be added to lfu tracker and mapper
        FrequencyMapper.put(key, 1);
        if (minimumFrequency == 1)
            FrequencyTracker.get(minimumFrequency).add(key);
        else {
            minimumFrequency = 1;
            LinkedHashSet<String> newLinkedHashSet = new LinkedHashSet<>();
            newLinkedHashSet.add(key);
            FrequencyTracker.put(minimumFrequency, newLinkedHashSet);
        }
    }
}

