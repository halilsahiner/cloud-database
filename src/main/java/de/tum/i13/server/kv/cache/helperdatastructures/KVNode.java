package de.tum.i13.server.kv.cache.helperdatastructures;

/**
 * A helper node structure to keep track of nodes in the DoublyLinkedList
 */
public class KVNode {
    public final String Key;
    public String Value;
    KVNode prev;
    KVNode next;

    public KVNode(String key, String value) {
        this.Value = value;
        Key = key;
    }
}

