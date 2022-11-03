package de.tum.i13.shared;

import org.javatuples.Pair;

import java.util.HashMap;

public class MetadataHelper {

    public static HashMap<String, Pair<String, String>> parseMetadata(String keyRanges) {
        HashMap<String, Pair<String, String>> metadata = new HashMap<>();

        String[] words = keyRanges.split(";");
        for (String keyRange : words) {
            String[] values = keyRange.split(",");
            metadata.put(values[2], new Pair<>(values[0], values[1]));
        }
        return metadata;
    }

    public static String getHostAddress(String hostAndPort) {
        hostAndPort = hostAndPort.trim();
        String[] addresses = hostAndPort.split(":");
        return addresses[0];
    }

    public static String getHostPort(String hostAndPort) {
        hostAndPort = hostAndPort.trim();
        String[] addresses = hostAndPort.split(":");
        return addresses[1];
    }

    public static Pair<String, Pair<String, String>> parseServerInfo(String serverInfo) {
        String[] values = serverInfo.split(",");
        Pair<String, String> range = new Pair<>(values[0], values[1]);
        String addressOfRange = values[2];
        return new Pair<>(addressOfRange, range);
    }

    public static void addKeyrangeInfo(String startOfRange, String endOfRange, String address, StringBuilder keyrange) {
        keyrange.append(startOfRange);
        keyrange.append(",");
        keyrange.append(endOfRange);
        keyrange.append(",");
        keyrange.append(address);
        keyrange.append(";");
    }
}
