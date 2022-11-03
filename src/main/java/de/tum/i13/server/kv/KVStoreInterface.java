package de.tum.i13.server.kv;

public interface KVStoreInterface {

    /**
     * Inserts a key-value pair into the KVServer.
     *
     * @param key   the key that identifies the given value.
     * @param value the value that is indexed by the given key.
     * @return a message that confirms the insertion of the tuple or an error.
     */
    KVMessage put(String key, String value);

    /**
     * Retrieves the value for a given key from the KVServer.
     *
     * @param key the key that identifies the value.
     * @return the value, which is indexed by the given key.
     */
    KVMessage get(String key);

    /**
     * Deletes the value for a given key from the Cache.
     *
     * @param key the key that identifies the value.
     * @return the value, which is indexed by the given key.
     */
    KVMessage delete(String key);
}
