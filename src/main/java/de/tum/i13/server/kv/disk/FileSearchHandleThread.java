package de.tum.i13.server.kv.disk;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

/**
 * This class search the given file with FileChannel.read function.
 * It is used in threads and  not sharing any variables between threads to search in files.
 * The number of threads this class called can be increased, if the processor is under-used.
 */
public class FileSearchHandleThread implements Runnable {

    private final String fileName;
    private final String key;
    private boolean isKeyFound = false;
    private boolean isDeletedKey = false;
    private int readPos = -1;
    private int readLen = -1;

    public FileSearchHandleThread(String fileName, String key) {
        this.fileName = fileName;
        this.key = key;
    }

    @Override
    public void run() {
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
            String keyWithSpace = key + " ";
            for (int i = lines.length - 1; 0 <= i; i--) {
                if (lines[i].startsWith(keyWithSpace)) {
                    isKeyFound = true;
                    String[] keyTokens = lines[i].split(" ");
                    if (keyTokens.length == 2) {
                        isDeletedKey = true;
                        return;
                    }
                    readPos = Integer.parseInt(keyTokens[1]);
                    readLen = Integer.parseInt(keyTokens[2]);
                    return;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isKeyFound() {
        return isKeyFound;
    }

    public int getReadPos() {
        return readPos;
    }

    public boolean isDeletedKey() {
        return isDeletedKey;
    }

    public int getReadLen() {
        return readLen;
    }
}
