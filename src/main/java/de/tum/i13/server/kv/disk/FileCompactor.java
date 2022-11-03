package de.tum.i13.server.kv.disk;

import de.tum.i13.shared.Config;
import de.tum.i13.shared.Constants;
import de.tum.i13.shared.HashConverter;
import de.tum.i13.shared.MetadataHandler;
import org.javatuples.Pair;
import org.javatuples.Triplet;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

public class FileCompactor {
    // <Key, <Pos, Length, isDeleted>
    private final HashMap<String, Triplet<Integer, Integer, Boolean>> uniqueKeys;
    // <FileName, <Key1, Key2>
    private final HashMap<Integer, List<String>> fileKeyMap;
    private final List<Integer> prevFileNameList;
    private final List<Integer> currentFileNameList;
    private final Config config;
    private final Logger logger;
    private final Pair<String, String> readRange;
    private long lastFileSize = 0L;

    public FileCompactor(List<Integer> prevFileNameList, Config config, Logger logger) {
        this.config = config;
        this.logger = logger;
        this.prevFileNameList = prevFileNameList;
        currentFileNameList = new ArrayList<>();
        uniqueKeys = new HashMap<>();
        fileKeyMap = new HashMap<>();
        readRange = MetadataHandler.getReadRange();
    }

    public Pair<List<Integer>, Long> compact(boolean doesReplacePreviousFiles) {
        int bufferSize = 1024;
        for (int i = prevFileNameList.size() - 1; 0 <= i; i--) {
            String keyFile = config.dataDir + "/" + i + Constants.KEY_EXTENSION;

            try (RandomAccessFile reader = new RandomAccessFile(keyFile, "r");
                 FileChannel channel = reader.getChannel();
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                if (bufferSize > channel.size()) {
                    bufferSize = (int) channel.size();
                }
                ByteBuffer buff = ByteBuffer.allocate(bufferSize);

                while (channel.read(buff) > 0) {
                    out.write(buff.array(), 0, buff.position());
                    buff.clear();
                }

                String fileContent = out.toString(StandardCharsets.ISO_8859_1);
                String[] lines = fileContent.split("\n");
                List<String> keysFromTheFile = new ArrayList<>();
                for (int j = lines.length - 1; 0 <= j; j--) {
                    if (!lines[i].isEmpty()) {
                        String[] keyTokens = lines[j].split(" ");
                        // Check whether the key in the current range of the server
                        // to clean the unused kv-pairs in the server
                        if (!uniqueKeys.containsKey(keyTokens[0]) &&
                                HashConverter.isInKeyRange(readRange, keyTokens[0])) {
                            if (keyTokens.length == 2)
                                uniqueKeys.put(keyTokens[0], new Triplet<>(-1,
                                        -1, true));
                            else
                                uniqueKeys.put(keyTokens[0], new Triplet<>(Integer.parseInt(keyTokens[1]),
                                        Integer.parseInt(keyTokens[2]), false));
                            keysFromTheFile.add(keyTokens[0]);
                        }
                    }
                }
                fileKeyMap.put(i, keysFromTheFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        // This loop will transfer the most recent versions of all the K-V Pairs in different files to
        // the generated files for deletion of stale KV pairs in the disk.
        try {
            RandomAccessFile keyWriter = new RandomAccessFile(config.dataDir + "/temp0" + Constants.KEY_EXTENSION, "rw");
            RandomAccessFile valueWriter = new RandomAccessFile(config.dataDir + "/temp0" + Constants.VALUE_EXTENSION, "rw");
            FileChannel newKeyChannel = keyWriter.getChannel();
            FileChannel newValueChannel = valueWriter.getChannel();
            ByteBuffer byteBuffer;
            currentFileNameList.add(0);
            for (HashMap.Entry<Integer, List<String>> entry : fileKeyMap.entrySet()) {
                RandomAccessFile valueReader = new RandomAccessFile(config.dataDir + "/" + entry.getKey().toString() + Constants.VALUE_EXTENSION, "r");
                FileChannel valueChannel = valueReader.getChannel();

                for (String key : entry.getValue()) {
                    // if key is deleted, no need to do anything for the new files
                    if (!uniqueKeys.get(key).getValue2()) {
                        int position = uniqueKeys.get(key).getValue0();
                        int length = uniqueKeys.get(key).getValue1();
                        if (lastFileSize + length >= Constants.FILE_SIZE_LIMIT) {
                            newKeyChannel.close();
                            newValueChannel.close();
                            keyWriter.close();
                            valueWriter.close();
                            lastFileSize = 0L;

                            String newFileName = config.dataDir + "/temp" + currentFileNameList.size();
                            keyWriter = new RandomAccessFile(newFileName + Constants.KEY_EXTENSION, "rw");
                            valueWriter = new RandomAccessFile(newFileName + Constants.VALUE_EXTENSION, "rw");
                            newKeyChannel = keyWriter.getChannel();
                            newValueChannel = valueWriter.getChannel();
                            currentFileNameList.add(currentFileNameList.size());
                        } else {
                            byteBuffer = ByteBuffer.wrap((key + " " + newValueChannel.size() + " " + length + "\n").getBytes(StandardCharsets.ISO_8859_1));
                            newKeyChannel.write(byteBuffer);
                            valueChannel.transferTo(position, length, newValueChannel);
                            lastFileSize += length;
                        }
                    }
                }
                valueChannel.close();
                valueReader.close();
            }
            newKeyChannel.close();
            newValueChannel.close();
            keyWriter.close();
            valueWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (doesReplacePreviousFiles) {
            for (int fileName : prevFileNameList) {
                String keyFilePath = String.format(config.dataDir + "/%d" + Constants.KEY_EXTENSION, fileName);
                String valueFilePath = String.format(config.dataDir + "/%d" + Constants.VALUE_EXTENSION, fileName);
                try {
                    Files.deleteIfExists(Paths.get(keyFilePath));
                    Files.deleteIfExists(Paths.get(valueFilePath));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            for (int tempFileName : currentFileNameList) {
                String tempKeyFilePath = String.format(config.dataDir + "/temp%d" + Constants.KEY_EXTENSION, tempFileName);
                String tempValueFilePath = String.format(config.dataDir + "/temp%d" + Constants.VALUE_EXTENSION, tempFileName);
                String newKeyFilePath = String.format(config.dataDir + "/%d" + Constants.KEY_EXTENSION, tempFileName);
                String newValueFilePath = String.format(config.dataDir + "/%d" + Constants.VALUE_EXTENSION, tempFileName);

                File keyFile = new File(tempKeyFilePath);
                File keyRename = new File(newKeyFilePath);

                File valueFile = new File(tempValueFilePath);
                File valueRename = new File(newValueFilePath);

                if (!(keyFile.renameTo(keyRename) && valueFile.renameTo(valueRename))) {
                    logger.severe("Replacing files during compaction had an error at renaming temp files.");
                }

            }
        }
        logger.info("Compaction is finished. Current files are: " + currentFileNameList);
        return new Pair<>(currentFileNameList, lastFileSize);
    }

    public void deleteSelectedFiles(List<Integer> fileListToDelete) {
        for (int fileName : fileListToDelete) {
            String keyFilePath = String.format(config.dataDir + "/%d" + Constants.KEY_EXTENSION, fileName);
            String valueFilePath = String.format(config.dataDir + "/%d" + Constants.VALUE_EXTENSION, fileName);
            try {
                Files.deleteIfExists(Paths.get(keyFilePath));
                Files.deleteIfExists(Paths.get(valueFilePath));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
