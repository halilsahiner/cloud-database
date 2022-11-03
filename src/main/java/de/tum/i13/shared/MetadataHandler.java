package de.tum.i13.shared;

import org.javatuples.Pair;

import java.util.ArrayList;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

import static de.tum.i13.shared.MetadataHelper.parseServerInfo;

public class MetadataHandler {

    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    // key(ip:port) -> value(hash-1 to hash-2)
    // key(ip:port) -> value(hash-2 to hash-3)
    private static final TreeMap<String, Pair<String, String>> metadata = new TreeMap<>(new AddressComparator());
    public static Logger logger = Logger.getLogger(MetadataHandler.class.getName());
    // The range of data that KV-Server is responsible for their put delete and get operations
    private static Pair<String, String> coordinatedRange;

    // The ranges of data that KV-Server is responsible for their get operations
    private static Pair<String, String> readRange;

    // The addresses of KV-Servers that the current KV-Server replicates its data in
    private static String coordinatedServer1;
    private static String coordinatedServer2;

    private static String currentAddress;

    private static String ecsIP;

    public static SortedMap<String, Pair<String, String>> getMetadata() {
        return metadata;
    }

    public static void updateMetadata(String keyRanges, String address) {
        lock.writeLock().lock();
        metadata.clear();
        currentAddress = address;

        String[] words = keyRanges.split(";");
        int rangeSize = words.length;
        int indexOfAddress = -1;
        for (int i = 0; i < rangeSize; i++) {
            Pair<String, Pair<String, String>> serverInfo = parseServerInfo(words[i]);
            metadata.put(serverInfo.getValue0(), serverInfo.getValue1());

            if (serverInfo.getValue0().equals(address)) {
                indexOfAddress = i;
                coordinatedRange = serverInfo.getValue1();
            }
        }
        if (rangeSize > 2) {
            String startOfReadRange = indexOfAddress - 2 < 0 ?
                    parseServerInfo(words[rangeSize + (indexOfAddress - 2)]).getValue1().getValue0() :
                    parseServerInfo(words[indexOfAddress - 2]).getValue1().getValue0();
            String endOfReadRange = coordinatedRange.getValue1();
            readRange = new Pair<>(startOfReadRange, endOfReadRange);

            coordinatedServer1 = indexOfAddress == rangeSize - 1 ?
                    parseServerInfo(words[0]).getValue0() :
                    parseServerInfo(words[indexOfAddress + 1]).getValue0();

            coordinatedServer2 = indexOfAddress + 2 >= rangeSize ?
                    parseServerInfo(words[(indexOfAddress + 2) % rangeSize]).getValue0() :
                    parseServerInfo(words[indexOfAddress + 2]).getValue0();
        } else {
            // After an update, if the server number is down to 2 or less, we shouldn't allow replication functions to work.
            coordinatedServer1 = null;
            coordinatedServer2 = null;
            readRange = coordinatedRange;
        }
        lock.writeLock().unlock();
    }

    public static String getKeyrange() {
        StringBuilder keyrange = new StringBuilder(" ");
        lock.readLock().lock();
        for (Map.Entry<String, Pair<String, String>> mapEntry : metadata.entrySet()) {
            MetadataHelper.addKeyrangeInfo(
                    mapEntry.getValue().getValue0(),
                    mapEntry.getValue().getValue1(),
                    mapEntry.getKey(),
                    keyrange);
        }
        lock.readLock().unlock();
        return keyrange.toString();
    }

    public static String getKeyrangeRead() {
        StringBuilder keyrangeRead = new StringBuilder(" ");
        lock.readLock().lock();
        int rangeSize = metadata.size();
        boolean isReplicationEnabled = rangeSize > 2;
        ArrayList<Map.Entry<String, Pair<String, String>>> mapEntries = new ArrayList<>(metadata.entrySet());
        if (isReplicationEnabled) {
            int index = 0;
            for (Map.Entry<String, Pair<String, String>> mapEntry : mapEntries) {
                String startOfReadRange = index - 2 < 0 ?
                        mapEntries.get(rangeSize + (index - 2)).getValue().getValue0() :
                        mapEntries.get(index - 2).getValue().getValue0();
                MetadataHelper.addKeyrangeInfo(
                        startOfReadRange,
                        mapEntry.getValue().getValue1(),
                        mapEntry.getKey(),
                        keyrangeRead);
                index++;
            }
        } else {
            for (Map.Entry<String, Pair<String, String>> mapEntry : metadata.entrySet()) {
                MetadataHelper.addKeyrangeInfo(
                        mapEntry.getValue().getValue0(),
                        mapEntry.getValue().getValue1(),
                        mapEntry.getKey(),
                        keyrangeRead);
            }
        }
        lock.readLock().unlock();
        return keyrangeRead.toString();
    }

    public static Pair<String, String> getCoordinatedRange() {
        Pair<String, String> currentCoordinatedRange;
        lock.readLock().lock();
        currentCoordinatedRange = coordinatedRange;
        lock.readLock().unlock();
        return currentCoordinatedRange;
    }

    public static Pair<String, String> getReadRange() {
        Pair<String, String> currentReplicatedRange1;
        lock.readLock().lock();
        currentReplicatedRange1 = readRange;
        lock.readLock().unlock();
        return currentReplicatedRange1;
    }


    public static String getCoordinatedServer1() {
        String currentCoordinatedServer1;
        lock.readLock().lock();
        currentCoordinatedServer1 = coordinatedServer1;
        lock.readLock().unlock();
        return currentCoordinatedServer1;
    }

    public static String getCoordinatedServer2() {
        String currentCoordinatedServer2;
        lock.readLock().lock();
        currentCoordinatedServer2 = coordinatedServer2;
        lock.readLock().unlock();
        return currentCoordinatedServer2;
    }

    public static String getSuccessor(AtomicBoolean isThereAnElection) {
        // TODO if the ecs is our successor, we need to find the successor of the ecs in case of the ecs failure
        String successor;
        lock.readLock().lock();
        String higherKey = metadata.higherKey(currentAddress);
        successor = higherKey != null ? higherKey : metadata.firstKey();
        // if the ecs is in the ring but the ring is not updated due to hard shutdown of ecs,
        // the election message should be pass from the ecs's predecessor to its successor.
        if(ecsIP.equals(successor.split(":")[0])) {
            higherKey = metadata.higherKey(successor);
            successor = higherKey != null ? higherKey : metadata.firstKey();
        }
        lock.readLock().unlock();
        return successor;
    }

    public static String getEcsIP() {
        String tempEcsIp;

        lock.readLock().lock();
        tempEcsIp = ecsIP;
        lock.readLock().unlock();

        return tempEcsIp;
    }

    public static void setEcsIP(String ecsIP) {

        lock.writeLock().lock();
        if (MetadataHandler.ecsIP != null){
            String tempECSAddress = null;
            for (String key : metadata.keySet()) {
                if (key.startsWith(MetadataHandler.ecsIP)) {
                    tempECSAddress = MetadataHandler.ecsIP;
                }
            }
            if(tempECSAddress != null) {
                metadata.remove(tempECSAddress);
            }
        }
        MetadataHandler.ecsIP = ecsIP;
        lock.writeLock().unlock();
    }

    public static String getCurrentAddress() {
        return currentAddress;
    }
}
