package de.tum.i13.server.kv.cache;

import de.tum.i13.server.kv.KVMessage;
import de.tum.i13.server.kv.KVStoreInterface;
import de.tum.i13.server.kv.Message;
import de.tum.i13.server.kv.cache.helperdatastructures.DoublyLinkedList;
import de.tum.i13.server.kv.cache.helperdatastructures.KVNode;

import java.util.HashMap;

/**
 * A cache implementation with an LRU strategy with the put(add & update), get and delete functionality
 */
public class DataCacheLRUStore implements KVStoreInterface {

    private final HashMap<String, KVNode> LRUCache;
    private final DoublyLinkedList LRUTracker;
    private final int sizeOfCache;
    private int currentSizeOfCache = 0;

    public DataCacheLRUStore(int sizeOfCache) {
        if (sizeOfCache < 0) {
            throw new IllegalArgumentException("Illegal cache capacity: " + sizeOfCache);
        }
        this.sizeOfCache = sizeOfCache;
        LRUCache = new HashMap<>();
        LRUTracker = new DoublyLinkedList();
    }

    @Override
    public KVMessage put(String key, String value) {
        boolean isUpdated;
        if (!LRUCache.containsKey(key)) {
            isUpdated = false;
            // if the cache is full, remove the pair that is first inserted
            if (currentSizeOfCache == sizeOfCache) {
                LRUCache.remove(LRUTracker.removeLast());
            } else {
                // if cache is not full, the current cache size should increase
                currentSizeOfCache++;
            }
            // new key should be added to fifo tracker
            KVNode newKVNode = LRUTracker.addNode(key, value);
            LRUCache.put(key, newKVNode);
        } else {
            KVNode KVNodeToMove = LRUCache.get(key);
            isUpdated = KVNodeToMove.Value != null;
            LRUTracker.moveNodeToFront(KVNodeToMove);
            KVNodeToMove.Value = value;
        }
        return new Message(key, value, isUpdated ? KVMessage.StatusType.PUT_UPDATE : KVMessage.StatusType.PUT_SUCCESS);
    }

    @Override
    public KVMessage get(String key) {
        if (!LRUCache.containsKey(key)) {
            return new Message(key, null, KVMessage.StatusType.GET_ERROR);
        } else {
            KVNode KVNodeToMove = LRUCache.get(key);
            LRUTracker.moveNodeToFront(KVNodeToMove);

            // if the key is found, but it is recently deleted by client, the cache returns a deleted status
            return new Message(key, KVNodeToMove.Value, KVNodeToMove.Value == null ? KVMessage.StatusType.GET_DELETED : KVMessage.StatusType.GET_SUCCESS);
        }
    }

    @Override
    public KVMessage delete(String key) {
        if (!LRUCache.containsKey(key) || LRUCache.get(key) == null) {
            return new Message(key, null, KVMessage.StatusType.DELETE_ERROR);
        }
        // The null value is a tombstone for the cache to give a fast get response of recently deleted values
        KVNode KVNodeToMove = LRUCache.get(key);
        LRUTracker.moveNodeToFront(KVNodeToMove);
        KVNodeToMove.Value = null;

        return new Message(key, null, KVMessage.StatusType.DELETE_SUCCESS);
    }
}
