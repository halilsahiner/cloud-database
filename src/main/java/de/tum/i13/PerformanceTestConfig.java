package de.tum.i13;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import static de.tum.i13.PerformanceTestRunner.getListOfFiles;
import static de.tum.i13.PerformanceTestRunner.runTests;

public class PerformanceTestConfig {
    private final static Logger LOGGER = Logger.getLogger(PerformanceTestConfig.class.getName());

    public static void main(String[] args) throws InterruptedException {

        //String pathPrefix = "/Users/ibrahimk/Downloads/data/maildir/";
        String pathPrefix = "/mnt/mydata/maildir/";

        int serverCount = Integer.parseInt(args[0]);

        int clientNum = Integer.parseInt(args[1]);
        String logPath = args[2];

        String[] people = getListOfFiles(pathPrefix);
        ArrayList<String> peopleList = new ArrayList<>(Arrays.asList(people));


        Collections.sort(peopleList);
        // Mac additional file removal
        if (peopleList.get(0).equals(".DS_Store")) {
            peopleList.remove(0);
        }
        System.out.println("Size of people : " + peopleList.size());
        System.out.println("First person : " + peopleList.get(0));

        Integer[] array = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        List<Integer> peopleIndex = Arrays.asList(array);
        Collections.shuffle(peopleIndex);

        List<Thread> listOfThreads = new ArrayList<>();
        for (int i = 0; i < clientNum; i++) {
            int clientIndex = i;
            Thread thread = new Thread(() -> {
                int trial = 0;
                while (trial < 10) {
                    runTests(peopleList.get(peopleIndex.get(trial)), pathPrefix + peopleList.get(peopleIndex.get(trial)) + "/all_documents", "put", clientIndex, serverCount, logPath + "-put");
                    trial++;
                }
            });
            thread.start();
            listOfThreads.add(thread);
        }
        for (Thread t :
                listOfThreads) {
            t.join();
        }
//
//
//        new Thread(() -> {
//            int trial = 0;
//            while (trial < 10) {
//                runTests(peopleList.get(peopleIndex.get(trial)), pathPrefix + peopleList.get(peopleIndex.get(trial)) + "/all_documents", "get", clientNum, serverCount, logPath + "-get");
//                trial++;
//            }
//        }).start();


    }

}
