package de.tum.i13.server.kv.disk;

import de.tum.i13.server.kv.KVMessage;
import de.tum.i13.server.kv.KVStoreInterface;
import de.tum.i13.server.kv.Message;
import de.tum.i13.shared.Config;
import de.tum.i13.shared.Constants;
import de.tum.i13.shared.HashConverter;
import org.javatuples.Pair;
import org.javatuples.Quartet;

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
import java.util.List;
import java.util.logging.Logger;

public class DataDiskStore implements KVStoreInterface {
    private final Logger logger;
    private final Config config;

    private List<Integer> fileNameRecord;
    private long currentValueFileSize;
    private long compactionCounter = 0;
    private int numberOfFileGroups = 0;

    public DataDiskStore(Config config, Logger logger) {
        this.logger = logger;
        this.config = config;
        fileNameRecord = new ArrayList<>();
        if (!Files.exists(config.dataDir)) {

            try {
                Files.createDirectory(config.dataDir);
                createNewFile();
            } catch (IOException e) {
                logger.severe("Could not create directory");
                System.exit(-1);
            }
        } else {
            File directoryPath = new File(config.dataDir.toString());
            File[] filesList = directoryPath.listFiles();
            if (filesList != null) {
                for (File file : filesList) {
                    try {
                        Files.deleteIfExists(file.toPath());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            if (numberOfFileGroups == 0) {
                createNewFile();
            } else {
                File currentFile = new File(config.dataDir + "/" + fileNameRecord.get(numberOfFileGroups - 1) + Constants.VALUE_EXTENSION);
                currentValueFileSize = currentFile.length();
                if (!(numberOfFileGroups == 1 && currentValueFileSize == 0)) {
                    startCompaction();
                }
            }
        }
    }

    @Override
    public KVMessage put(String key, String value) {
        // This function appends the k-v pair to the most recent file and position
        try {

            Quartet<Integer, Integer, Integer, Boolean> fileWithTheRecentValue = getFileWithTheRecentValue(key);
            int candidateFileToPut = getCandidateFileToPut();

            String keyFileName = config.dataDir + "/" + candidateFileToPut + Constants.KEY_EXTENSION;
            String valueFileName = config.dataDir + "/" + candidateFileToPut + Constants.VALUE_EXTENSION;
            // Since the FileChannel.size cannot read the size of file with a given charset,
            // the location should be byte based
            long currentPosition = currentValueFileSize;
            appendStringToFile(keyFileName, key + " " + currentPosition + " " + value.getBytes().length + "\n");
            appendStringToFile(valueFileName, value);
            currentValueFileSize += value.getBytes().length;

            if (fileWithTheRecentValue.getValue0() != -1 && !fileWithTheRecentValue.getValue3()) {
                logger.info("Key " + key + " updated with value: " + value + " successfully.");
                increaseCompactionCounter(value.getBytes().length);
                return new Message(key, value, KVMessage.StatusType.PUT_UPDATE);
            } else {
                logger.info("Key: " + key + ", value: " + value + " pair added successfully.");
                return new Message(key, value, KVMessage.StatusType.PUT_SUCCESS);
            }
        } catch (RuntimeException | IOException e) {
            logger.severe("Value can not be added/updated. Caused by: " + e.getMessage());
            return new Message(key, value, KVMessage.StatusType.PUT_ERROR);
        }
    }

    @Override
    public KVMessage get(String key) {
        Message message;
        logger.info("Starting get operation for key : " + key);
        try {
            Quartet<Integer, Integer, Integer, Boolean> fileWithTheRecentValue = getFileWithTheRecentValue(key);
            if (fileWithTheRecentValue.getValue0() != -1 && !fileWithTheRecentValue.getValue3()) {
                String value = readFromAPosition(config.dataDir + "/" + fileWithTheRecentValue.getValue0() + Constants.VALUE_EXTENSION,
                        fileWithTheRecentValue.getValue1(), fileWithTheRecentValue.getValue2());
                message = new Message(key, value, KVMessage.StatusType.GET_SUCCESS);
                logger.info("Key " + key + " read successfully. Value: " + value);
            } else if (fileWithTheRecentValue.getValue0() != -1) {
                message = new Message(key, null, KVMessage.StatusType.GET_DELETED);
                logger.info("Key " + key + " is recently deleted");
            } else {
                logger.info("Value is not found");
                message = new Message(key, null, KVMessage.StatusType.GET_ERROR);
            }
        } catch (IOException | RuntimeException e) {
            logger.warning("Value can not be read. Caused by: " + e.getMessage());
            message = new Message(key, null, KVMessage.StatusType.GET_ERROR);
        }
        return message;
    }

    @Override
    public KVMessage delete(String key) {
        try {
            Quartet<Integer, Integer, Integer, Boolean> fileWithTheRecentValue = getFileWithTheRecentValue(key);


            // Since the FileChannel.size cannot read the size of file with a given charset,
            // the location should be byte based
            if (fileWithTheRecentValue.getValue0() != -1 && !fileWithTheRecentValue.getValue3()) {
                deleteKey(key);

                increaseCompactionCounter(fileWithTheRecentValue.getValue2());
                logger.info("Key " + key + " deleted successfully.");
                return new Message(key, null, KVMessage.StatusType.DELETE_SUCCESS);
            } else {
                logger.info("Key: " + key + "does not found");
                return new Message(key, null, KVMessage.StatusType.DELETE_ERROR);
            }
        } catch (RuntimeException | IOException e) {
            logger.severe("Value can not be added/updated. Caused by: " + e.getMessage());
            return new Message(key, null, KVMessage.StatusType.DELETE_ERROR);
        }
    }

    public int getNumberOfFileGroups() {
        return numberOfFileGroups;
    }

    public void deleteKey(String key) throws IOException {
        int candidateFileToPut = getCandidateFileToPut();
        String keyFileName = config.dataDir + "/" + candidateFileToPut + Constants.KEY_EXTENSION;
        String deleteLine = key + " *\n";
        appendStringToFile(keyFileName, deleteLine);
    }

    public void deleteAll() {
        for (int fileName : fileNameRecord) {
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

    public Pair<ArrayList<Quartet<Integer, String, Integer, Integer>>, Integer> getNextSetOfKeyInfoInRange(String rangeStart, String rangeEnd, int lastSearchedFile) {
        ArrayList<Quartet<Integer, String, Integer, Integer>> setOfKeyInfoInRange = new ArrayList<>();
        logger.fine("Started searching for existing key in disk");
        if (lastSearchedFile == numberOfFileGroups) {
            if (compactionCounter > 0L)
                startCompaction();
        }
        while (setOfKeyInfoInRange.size() == 0 && lastSearchedFile != 0) {
            if (lastSearchedFile == -1)
                logger.warning("should not happen");
            setOfKeyInfoInRange = searchSetOfKeyInfoInRange(fileNameRecord.get(lastSearchedFile - 1), rangeStart, rangeEnd);
            lastSearchedFile--;
        }
        logger.fine("Finished key search in the disk.");
        return new Pair<>(setOfKeyInfoInRange, lastSearchedFile);
    }

    private ArrayList<Quartet<Integer, String, Integer, Integer>> searchSetOfKeyInfoInRange(Integer fileRecordName, String rangeStart, String rangeEnd) {
        ArrayList<Quartet<Integer, String, Integer, Integer>> setOfKeyInfoInRange = new ArrayList<>();
        String fileName = config.dataDir + "/" + fileRecordName + Constants.KEY_EXTENSION;
        try (RandomAccessFile reader = new RandomAccessFile(fileName, "r");
             FileChannel channel = reader.getChannel();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            int bufferSize = 1024;
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

            for (int i = lines.length - 1; 0 <= i; i--) {
                if (!lines[i].isEmpty()) {
                    String[] keyTokens = lines[i].split(" ");
                    if (HashConverter.isInKeyRange(new Pair<>(rangeStart, rangeEnd), keyTokens[0])) {
                        setOfKeyInfoInRange.add(new Quartet<>(fileRecordName, keyTokens[0],
                                Integer.parseInt(keyTokens[1]), Integer.parseInt(keyTokens[2])));
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return setOfKeyInfoInRange;
    }

    /**
     * Increases the compaction counter until it reaches two times of the file limit of the system. Then it starts the
     * compaction process of the disk files.
     *
     * @param updatedSize the size of the update for that key that exist in the disk already
     */
    private void increaseCompactionCounter(long updatedSize) {
        compactionCounter += updatedSize;
        if (compactionCounter == 2L * Constants.FILE_SIZE_LIMIT) {
            logger.info("Compaction started.");
            startCompaction();
        }
    }

    private void startCompaction() {
        compactionCounter = 0L;
        FileCompactor compactor = new FileCompactor(fileNameRecord, config, logger);
        Pair<List<Integer>, Long> compactionResult = compactor.compact(true);
        fileNameRecord = compactionResult.getValue0();
        currentValueFileSize = compactionResult.getValue1();
    }

    private void appendStringToFile(String filePath, String stringToAppend) throws IOException {
        try (RandomAccessFile writer = new RandomAccessFile(filePath, "rw");
             FileChannel channel = writer.getChannel()) {
            channel.position(channel.size());
            ByteBuffer buff = ByteBuffer.wrap(stringToAppend.getBytes(StandardCharsets.ISO_8859_1));
            channel.write(buff);
        }
    }

    public String readFromAPosition(String filePath, long position, int length) throws IOException {
        try (RandomAccessFile reader = new RandomAccessFile(filePath, "r");
             FileChannel channel = reader.getChannel();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            int bufferSize = length == 0 ? 1024 : length;
            if (bufferSize > channel.size()) {
                bufferSize = (int) channel.size();
            }
            ByteBuffer buff = ByteBuffer.allocate(bufferSize);
            if (position != 0L) {
                channel.position(position);
            }
            while (length != 0) {
                length -= channel.read(buff);
                out.write(buff.array(), 0, buff.position());
                buff.clear();
            }
            return out.toString();
        }
    }

    /**
     * Gets the most recent value position of a key through searching disk files in a multithreaded manner
     *
     * @param key the key value to search in files
     * @return the file name with the value, starting position of the value, length of the value and a flag for deleted keys
     * @throws RuntimeException when one of the File Search thread is interrupted
     */
    private Quartet<Integer, Integer, Integer, Boolean> getFileWithTheRecentValue(String key) throws RuntimeException {
        logger.fine("Started searching for existing key in disk");
        boolean isKeyFound = false;
        int fileWithTheKey = -1;
        int readPosition = -1;
        int readLen = -1;
        boolean isDeletedKey = false;
        int numberOfThreads = Constants.FILE_READ_THREAD_COUNT;
        FileSearchHandleThread[] fileSearchHandleThreads = new FileSearchHandleThread[numberOfThreads];
        Thread[] threads = new Thread[numberOfThreads];

        for (int i = numberOfFileGroups; 0 < i && !isKeyFound; ) {
            for (int j = 0; j < (Math.min(i, numberOfThreads)); j++) {
                fileSearchHandleThreads[j] = new FileSearchHandleThread(config.dataDir + "/" + fileNameRecord.get(i - j - 1) + Constants.KEY_EXTENSION, key);
                threads[j] = new Thread(fileSearchHandleThreads[j]);
                threads[j].start();
            }
            for (int j = 0; j < (Math.min(i, numberOfThreads)); j++) {
                try {
                    threads[j].join();
                } catch (InterruptedException e) {
                    logger.severe("One of the File Search thread is interrupted");
                    throw new RuntimeException(e);
                }
                if (fileSearchHandleThreads[j].isKeyFound() && !isKeyFound) {
                    isKeyFound = true;
                    fileWithTheKey = j;
                    readPosition = fileSearchHandleThreads[j].getReadPos();
                    readLen = fileSearchHandleThreads[j].getReadLen();
                    isDeletedKey = fileSearchHandleThreads[j].isDeletedKey();
                }
            }
            if (i <= numberOfThreads) {
                i = 0;
            } else {
                i -= numberOfThreads;
            }
        }
        logger.fine("Finished key search in the disk.");
        return new Quartet<>(fileWithTheKey, readPosition, readLen, isDeletedKey);
    }

    /**
     * Gets the file name of the file that put operation should add its value
     *
     * @return the file name for the file group for put
     * @throws RuntimeException Could not create a disk file
     */
    private int getCandidateFileToPut() throws RuntimeException {
        logger.fine("Getting the value file for adding new value.");
        if (currentValueFileSize <= Constants.FILE_SIZE_LIMIT - Constants.MESSAGE_SIZE_LIMIT)
            return numberOfFileGroups - 1;
        return createNewFile();
    }

    /**
     * Creates a new file for the DataDiskStore and add it to the watch list of store.
     *
     * @return the file name for the file group created
     * @throws RuntimeException Could not create a disk file
     */
    private int createNewFile() throws RuntimeException {
        logger.fine("New file creation is started.");
        int fileName = numberOfFileGroups;
        currentValueFileSize = 0;

        String keyFileNameWithExtension = "/" + fileName + Constants.KEY_EXTENSION;
        String valueFileNameWithExtension = "/" + fileName + Constants.VALUE_EXTENSION;

        File keyFile = new File(config.dataDir + keyFileNameWithExtension);
        File valueFile = new File(config.dataDir + valueFileNameWithExtension);
        try {
            if (!(keyFile.createNewFile() && valueFile.createNewFile())) {
                logger.severe("The file group already exist: " + fileName);
                return -1;
            }
        } catch (IOException e) {
            logger.severe("Could not create a disk file");
            throw new RuntimeException(e);
        }
        fileNameRecord.add(fileName);
        numberOfFileGroups++;

        logger.fine("New file creation is successful.");
        return fileName;
    }
}
