package de.tum.i13;

import de.tum.i13.server.kv.disk.FileCompactor;
import de.tum.i13.shared.Config;
import de.tum.i13.shared.MetadataHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.LogManager;

public class CompactorTest {
//
//    static Config config;
//
//    @BeforeAll
//    static void InitializeConfig() {
//        config = new Config();
//        config.dataDir = Paths.get("src/test/resources");
//        MetadataHandler.updateMetadata("00000000000000000000000000000000,00000000000000000000000000000000," + config.listenaddr + ":" + config.port + ";", config.listenaddr + ":" + config.port);
//
//    }
//
//    @AfterAll
//    static void deleteAllTestFiles() {
//        File directoryPath = new File(config.dataDir.toString());
//        if (directoryPath.exists()) {
//            File[] filesList = directoryPath.listFiles();
//            if (filesList != null && filesList.length != 0)
//                for (File f : filesList) {
//                    if (f.getName().startsWith("temp"))
//                        f.delete();
//                }
//        }
//    }
//
//    @Test
//    public void CompactKeys() throws IOException {
//        Files.deleteIfExists(Paths.get("src/test/resources/temp0.keys"));
//        Files.deleteIfExists(Paths.get("src/test/resources/temp0.values"));
//        List<Integer> fileNameList = new ArrayList<>();
//        fileNameList.add(0);
//        fileNameList.add(1);
//        fileNameList.add(2);
//
//        FileCompactor fileCompactor = new FileCompactor(fileNameList, config, LogManager.getLogManager().getLogger(""));
//        fileCompactor.compact(false);
//    }

}
