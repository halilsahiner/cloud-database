package de.tum.i13;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EmptyDirectoryEraser {

    public static void main(String[] args) {
        String dir = "/Users/ibrahimk/Downloads/data/maildir/";
        File directoryPath = new File(dir);


        File[] filesList = directoryPath.listFiles();

        List<String> deleteDirs = new ArrayList<>();
        if (filesList != null) {
            for (File file : filesList) {
                boolean doesContain = false;
                if (file.isDirectory()) {
                    for (File inside : file.listFiles()) {
                        if (inside.getName().equals("all_documents")) {
                            doesContain = true;
                        }
                    }
                }
                if (!doesContain) {
                    deleteDirs.add(file.toPath().toString());
                }

            }
            Collections.sort(deleteDirs);
            for (String s : deleteDirs)
                System.out.println(s);

        }
    }
}
