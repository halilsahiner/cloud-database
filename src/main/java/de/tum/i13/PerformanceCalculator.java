package de.tum.i13;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PerformanceCalculator {

    public static void main(String[] args) {
        File performanceDirectory = new File("G:\\ms3\\performance\\ms5");
        File[] performanceFiles = performanceDirectory.listFiles((dir, name) -> !(name.equals("maildir") || name.equals("olddata")));
        List<String> dataSent = new ArrayList<>();
        List<String> latency = new ArrayList<>();
        List<String> throughput = new ArrayList<>();
        if (performanceFiles != null) {
            for (File setupType : performanceFiles) {
                long sumOfTimings = 0;
                long numberOfTimings = 0;
                long sumOfSentData = 0;
                String setupTypeName = setupType.getName();
                File[] timingFiles = setupType.listFiles();
                if (timingFiles != null) {
                    for (File timingFile : timingFiles) {
                        try {
                            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(timingFile)));
                            String firstLine;

                            while ((firstLine = in.readLine()) != null) {
                                if (firstLine.startsWith("Test"))
                                    continue;
                                if (firstLine.startsWith("Valid"))
                                    continue;
                                String[] split = firstLine.split("\\s+");
                                sumOfTimings += Long.parseLong(split[0]);
                                if (split.length < 2) {
                                    System.out.println(firstLine + " " + timingFile.getName() + " " + setupTypeName);
                                }
                                if(firstLine.split("\\s+").length > 1) {
                                    sumOfSentData += Long.parseLong(firstLine.split("\\s+")[1]);
                                    numberOfTimings++;
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                if (sumOfTimings != 0 && numberOfTimings != 0) {
                    latency.add(setupTypeName + "\tlatency: " + (sumOfTimings / (numberOfTimings * 1000L)) + "\tmicrosecond ");
                    throughput.add(setupTypeName + "\tthroughput: " + numberOfTimings + ", " + ((numberOfTimings * 1000000000L) / sumOfTimings) + "\tper second");
                    dataSent.add(setupTypeName +"\tdata sent: " + sumOfSentData + ", " +  ((sumOfSentData * 1000000000L) / sumOfTimings ) + "\tbyte/per second");
                }
            }
        }
        Collections.sort(latency);
        for (String line : latency) {
            System.out.println(line);
        }
        Collections.sort(throughput);
        for (String line : throughput) {
            System.out.println(line);
        }
        Collections.sort(dataSent);
        for (String line : dataSent) {
            System.out.println(line);
        }
    }
}
