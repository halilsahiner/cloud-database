package de.tum.i13.ecs;

import de.tum.i13.shared.AddressComparator;
import de.tum.i13.shared.HashConverter;
import org.awaitility.core.ConditionTimeoutException;
import org.javatuples.Pair;

import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

import static org.awaitility.Awaitility.await;

public class ECSServer {
    private final static Logger logger = Logger.getLogger(ECSServer.class.getName());
    private final TreeMap<String, Pair<String, String>> ring = new TreeMap<>(new AddressComparator());
    private final HashMap<String, PrintWriter> KVServerStreams = new HashMap<>();
    private final BlockingQueue<String> heartbeats = new LinkedBlockingQueue<>();
    private final List<String> previousMetadata;

    public ECSServer(List<String> previousMetadata) {
        logger.info(String.valueOf(previousMetadata));
        this.previousMetadata = previousMetadata;
    }

    public void putKVServerToRing(String address) {
        String md5 = HashConverter.getMd5(address);
        if (ring.isEmpty()) {
            ring.put(address, new Pair<>(md5, md5));
        } else {
            ring.put(address, null);
            String successorAddress = getSuccessorAddress(address);
            String successorStart = ring.get(successorAddress).getValue0();
            ring.put(successorAddress, ring.get(successorAddress).setAt0(md5));
            ring.put(address, new Pair<>(successorStart, md5));
        }
    }

    public Pair<String, String> removeKVServerFromRing(String address) {
        if (ringSize() != 1) {
            String successorAddress = getSuccessorAddress(address);
            ring.put(successorAddress, ring.get(successorAddress).setAt0(ring.get(address).getValue0()));
        }
        return ring.remove(address);
    }

    public String getKeyRanges() {
        StringBuilder keyRanges = new StringBuilder();
        Set<Map.Entry<String, Pair<String, String>>> entrySet = ring.entrySet();

        for (Map.Entry<String, Pair<String, String>> m : entrySet) {
            String key = m.getKey();
            Pair<String, String> value = m.getValue();
            keyRanges.append(value.getValue0()).append(",").append(value.getValue1()).append(",").append(key).append(";");
        }
        return keyRanges + "\r\n";
    }

    public void initializationDataTransfer(String address) {
        String successorAddress = getSuccessorAddress(address);
        PrintWriter out = KVServerStreams.get(successorAddress);
        logger.info("set_write_lock transfer_data_init");
        out.write("set_write_lock\r\n");
        String dataTransferMessage = "transfer_data_init " + address + ";" + ring.get(address).getValue0() + ";" + ring.get(address).getValue1() + "\r\n";
        out.write(dataTransferMessage);
        out.flush();
    }

    public String removalDataTransfer(Pair<String, String> range, String successorAddress) {
        return "transfer_data_kill " + successorAddress + ";" + range.getValue0() + ";" + range.getValue1() + "\r\n";
    }

    public void sendKeyRangesToAll() {
        String keyRanges = "update_ranges " + getKeyRanges();
        for (PrintWriter out : KVServerStreams.values()) {
            out.write(keyRanges);
            out.flush();
        }
    }

    public void sendRemoveBlockElectionToAll() {
        String keyRanges = "remove_block\r\n";
        logger.info("The remove block sent to: " + KVServerStreams.size());
        for (PrintWriter out : KVServerStreams.values()) {
            out.write(keyRanges);
            out.flush();
        }
    }

    public void addKVServerToClientConnections(String address, PrintWriter out) {
        KVServerStreams.put(address, out);
    }

    public String getSuccessorAddress(String address) {
        String successorKey = ring.higherKey(address);
        return successorKey != null ? successorKey : ring.firstKey();
    }

    public void removeKVServerToClientConnections(String address) {
        KVServerStreams.remove(address);
    }

    public void sendMetadataToSuccessor(String successorAddress) {
        PrintWriter out = KVServerStreams.get(successorAddress);
        String keyRanges = "update_ranges " + getKeyRanges();
        out.write(keyRanges);
        out.flush();
    }

    public void sendHeartbeats(String ownIp) {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            Set<Map.Entry<String, PrintWriter>> entrySet = KVServerStreams.entrySet();
            for (Map.Entry<String, PrintWriter> m : entrySet) {
                String address = m.getKey();

                if (address.startsWith(ownIp)) {
                    continue;
                }

                PrintWriter out = m.getValue();
                CompletableFuture.runAsync(() -> {
                    out.write("ping_heartbeat\r\n");
                    out.flush();
                    try {
                        await().atMost(700, TimeUnit.MILLISECONDS).until(() -> heartbeats.contains(address));
                        heartbeats.remove(address);
                    } catch (ConditionTimeoutException e) {
                        logger.warning("Server is not responding!");
                        KVServerStreams.remove(address);
                        removeKVServerFromRing(address);
                        sendKeyRangesToAll();
                    }
                });
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public boolean isPreviousServersConnected() {
        for (String address : previousMetadata) {
            if (!ring.containsKey(address)) {
                return false;
            }
        }
        return true;
    }

    public HashMap<String, PrintWriter> getKVServerStreams() {
        return KVServerStreams;
    }

    public BlockingQueue<String> getHeartbeats() {
        return heartbeats;
    }

    public int ringSize() {
        return ring.size();
    }

}