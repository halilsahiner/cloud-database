package de.tum.i13;

import de.tum.i13.server.kv.disk.FileSearchHandleThread;
import de.tum.i13.shared.Constants;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FileSearchTest {
    @Test
    public void SearchARecentlyUpdatedKey() {
        int numberOfFileGroups = 3;
        String key = "key1";
        boolean isKeyFound = false;
        List<Integer> fileNameRecord = new ArrayList<>();
        fileNameRecord.add(0);
        fileNameRecord.add(1);
        fileNameRecord.add(2);
        Thread[] threads = new Thread[numberOfFileGroups];
        FileSearchHandleThread[] fileSearchHandleThreads = new FileSearchHandleThread[numberOfFileGroups];
        for (int i = 0; i < numberOfFileGroups; i++) {
            fileSearchHandleThreads[i] = new FileSearchHandleThread("src/test/resources/" + fileNameRecord.get(i) + Constants.KEY_EXTENSION, key);
            threads[i] = new Thread(fileSearchHandleThreads[i]);
            threads[i].start();
        }
        for (int i = numberOfFileGroups - 1; 0 <= i; i--) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (fileSearchHandleThreads[i].isKeyFound()) {
                if (isKeyFound) {
                    System.out.println("Key is already found");
                } else {
                    isKeyFound = true;
                    // Find deleted key
                    assertEquals(i, 2);
                    assertEquals(fileSearchHandleThreads[i].getReadPos(), -1);
                }
            }
        }
    }
}
