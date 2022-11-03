package de.tum.i13.server.kv.cache.helperdatastructures;

/**
 * A minimal DoublyLinkedList implementation for LRU cache implementation.
 */
public class DoublyLinkedList {
    public KVNode head = null;
    public KVNode tail = null;

    private int _size = 0;

    /**
     * Add a node at the beginning of the list
     *
     * @param key   KV pair key
     * @param value KV pair value
     * @return KVNode instance that added
     */
    public KVNode addNode(String key, String value) {
        KVNode newKVNode = new KVNode(key, value);
        _size++;

        if (head == null) {
            head = newKVNode;
            tail = newKVNode;
        } else {
            head.prev = newKVNode;
            newKVNode.next = head;
            head = newKVNode;
        }

        return newKVNode;
    }


    /**
     * Removes the last node of the list structure.
     *
     * @return the key of the node that removed from the list
     */
    public String removeLast() {
        if (_size == 0)
            throw new NullPointerException("No element in the list to remove");

        String key = tail.Key;
        if (tail.prev != null)
            tail.prev.next = null;
        tail = tail.prev;
        _size--;
        return key;
    }

    /**
     * Moves the given node to the head of the list
     *
     * @param KVNodeToMove the KVNode object that will be moved to the front
     */
    public void moveNodeToFront(KVNode KVNodeToMove) {
        if (KVNodeToMove == head)
            return;

        if (KVNodeToMove == tail) {
            tail = KVNodeToMove.prev;
        }

        KVNode prevNode = KVNodeToMove.prev;
        KVNode nextNode = KVNodeToMove.next;

        prevNode.next = nextNode;
        if (nextNode != null) {
            nextNode.prev = prevNode;
        }
        head.prev = KVNodeToMove;
        KVNodeToMove.next = head;
        KVNodeToMove.prev = null;
        head = KVNodeToMove;
    }
}