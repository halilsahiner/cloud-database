package de.tum.i13;

import de.tum.i13.client.CommandLineInterface;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Random;
import java.util.logging.Logger;

public class PerformanceTestRunner {

    private final static Logger LOGGER = Logger.getLogger(PerformanceTestRunner.class.getName());

    public static String[] getListOfFiles(String directory) {
        File files = new File(directory);
        return files.list();
    }

    public static void runTests(String person, String pathPrefix, String operation, int clientCount, int serverCount, String logPath) {

        String[] listOfFiles = getListOfFiles(pathPrefix);
        PrintWriter writer = null;

        System.out.println("Size of files : " + listOfFiles.length);
        System.out.println("First file : " + listOfFiles[0]);

        String dockerLogPath = "/mnt/mydata/ms5/" + logPath + "/";
        String localLogPath = "src/main/resources/";

        String dockerId = System.getenv("HOSTNAME");

        try {
            File newDir = new File(dockerLogPath);
            newDir.mkdir();
            writer = new PrintWriter(dockerLogPath + dockerId + "_" + operation + "_data_" + person + "_c" + clientCount + "_s" + serverCount + ".txt");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        writer.println("Test for " + operation + " with " + clientCount + " clients and " + serverCount + " servers");

        CommandLineInterface cli = new CommandLineInterface();

        int portOffset = new Random().nextInt(serverCount);

        String targetHost = "kv-server." + (1 + portOffset);
        String targetPort = "" + (5151 + portOffset);
        cli.connect(new String[]{"connect", targetHost, targetPort});

        for (String file : listOfFiles) {
            String filePath = pathPrefix + "/" + file;
            if (operation.equals("get")) {
                long startTime = System.nanoTime();
                String result = cli.kvOperation("get " + filePath);
                long timePassed = System.nanoTime() - startTime;
                writer.println(timePassed + " " + result.split("\\s+", 3)[2].getBytes().length);
            } else {
                try (RandomAccessFile reader = new RandomAccessFile(Paths.get(filePath).toFile(), "r");
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

                    String data = out.toString(StandardCharsets.ISO_8859_1);
                    data = data.replaceAll("\r", " ");
                    data = data.replaceAll("\n", " ");

                    if(data.length() > 120000L) {
                        continue;
                    }

                    String kvCommand;
                    if (operation.equals("put") || operation.equals("update")) { // put or update
                        kvCommand = "put" + " " + filePath + " " + data;
                    } else { // get or delete
                        kvCommand = operation + " " + file;
                    }

                    long startTime = System.nanoTime();
                    String result = cli.kvOperation(kvCommand);
                    long timePassed = System.nanoTime() - startTime;
                    writer.println(timePassed + " " + data.getBytes().length);

                    if (result.startsWith("put_error")) {
                        System.out.println("Operation failed : " + kvCommand);
                        writer.println("Invalid performance data");
                        writer.close();
                        break;
                    }

                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        }

        cli.disconnect();
        writer.println("Valid performance data");
        writer.close();
    }


}
