package de.tum.i13;

public class TestKVClientCommandProcessor {
/*
    static AtomicBoolean isServerWriteLocked = new AtomicBoolean(false);
    @Test
    public void correctParsingOfPut() {
        KVStoreInterface kv = mock(KVStoreInterface.class);
        KVStoreInterface cs = mock(KVStoreInterface.class);
        KVMessage diskMessage = mock(KVMessage.class);
        String strategy = "mock_strategy";
        when(kv.put(anyString(), anyString())).thenReturn(diskMessage);
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        KVClientCommandProcessor kvcp = new KVClientCommandProcessor(lock, kv, cs, strategy, isServerWriteLocked);
        kvcp.process("put key hello");

        verify(kv).put("key", "hello");
    }

    @Test
    public void correctParsingOfDelete() {
        KVStoreInterface kv = mock(KVStoreInterface.class);
        KVStoreInterface cs = mock(KVStoreInterface.class);
        KVMessage diskMessage = mock(KVMessage.class);
        String strategy = "mock_strategy";
        when(kv.delete(anyString())).thenReturn(diskMessage);
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        KVClientCommandProcessor kvcp = new KVClientCommandProcessor(lock, kv, cs, strategy, isServerWriteLocked);
        kvcp.process("delete key");

        verify(kv).delete("key");
    }

    @Test
    public void correctParsingOfGet() {
        KVStoreInterface kv = mock(KVStoreInterface.class);
        KVStoreInterface cs = mock(KVStoreInterface.class);
        KVMessage cacheMessage = mock(KVMessage.class);
        KVMessage diskMessage = mock(KVMessage.class);
        String strategy = "mock_strategy";
        when(cs.get(anyString())).thenReturn(cacheMessage);
        when(kv.get(anyString())).thenReturn(diskMessage);
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        KVClientCommandProcessor kvcp = new KVClientCommandProcessor(lock, kv, cs, strategy, isServerWriteLocked);
        kvcp.process("get key");

        verify(kv).get("key");
    }

    @Test
    public void keyComparisonInRange() {
        KVStoreInterface kv = mock(KVStoreInterface.class);
        KVStoreInterface cs = mock(KVStoreInterface.class);
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        KVClientCommandProcessor kvcp = new KVClientCommandProcessor(lock, kv, cs, "", isServerWriteLocked);

        assertTrue(kvcp.isInKeyRange(new Pair<>("0AB123","100CD0"),"0F"));
    }

    @Test
    public void keyComparisonOutOfRange() {
        KVStoreInterface kv = mock(KVStoreInterface.class);
        KVStoreInterface cs = mock(KVStoreInterface.class);
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        KVClientCommandProcessor kvcp = new KVClientCommandProcessor(lock, kv, cs, "", isServerWriteLocked);

        assertFalse(kvcp.isInKeyRange(new Pair<>("0AB123","100CD0"),"11"));
    }

*/
}
