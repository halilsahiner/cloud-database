package de.tum.i13;

import de.tum.i13.ecs.ECSServer;
import de.tum.i13.shared.Constants;
import org.javatuples.Pair;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.rules.ExpectedException;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;

public class ECSServerTest {
    @Rule
    public final ExpectedException exception = ExpectedException.none();
    ECSServer ecsServer;
    ServerSocket serverSocket;

    String ecsIp = "127.0.0.2";

    @BeforeEach
    public void createInstances() throws IOException {
        ecsServer = new ECSServer(new ArrayList<>());
        serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress(ecsIp, 5160));
    }

    @AfterEach
    public void closeInstances() throws IOException {
        serverSocket.close();
    }


    @Test
    public void getKeyRangesTest() {
        String address1 = "127.0.0.1:3343";
        String MD5Value1 = "ce887e439e7c12bc626688186ff2c51d";
        String address2 = "127.0.0.1:3344";
        String MD5Value2 = "9251e2d00e6ddb1a7572e2782d5146fa";
        String address3 = "127.0.0.1:3345";
        String MD5Value3 = "9fe0aaac6335b1cbf0a7aecef03d0c05";
        ecsServer.putKVServerToRing(address1);
        ecsServer.putKVServerToRing(address2);
        ecsServer.putKVServerToRing(address3);
        String actual = MD5Value1 + "," + MD5Value2 + "," + address2 + ";" + MD5Value2 + "," + MD5Value3 + "," + address3 + ";" + MD5Value3 + "," + MD5Value1 + "," + address1 + ";";

        Assertions.assertEquals(ecsServer.getKeyRanges(), actual + "\r\n");
    }

    @Test
    public void putKVServerToRingOneElementTest() {
        String address = "127.0.0.1:3343";
        String MD5Value = "ce887e439e7c12bc626688186ff2c51d";
        ecsServer.putKVServerToRing(address);

        Assertions.assertEquals(ecsServer.getKeyRanges(), MD5Value + "," + MD5Value + ",127.0.0.1:3343;\r\n");
    }

    @Test
    public void putKVServerToRingMultiElementTest() {
        String address1 = "127.0.0.1:3343";
        String MD5Value1 = "ce887e439e7c12bc626688186ff2c51d";
        String address2 = "127.0.0.1:3344";
        String MD5Value2 = "9251e2d00e6ddb1a7572e2782d5146fa";
        String address3 = "127.0.0.1:3345";
        String MD5Value3 = "9fe0aaac6335b1cbf0a7aecef03d0c05";
        ecsServer.putKVServerToRing(address1);
        ecsServer.putKVServerToRing(address2);
        ecsServer.putKVServerToRing(address3);
        String actual = MD5Value1 + "," + MD5Value2 + "," + address2 + ";" + MD5Value2 + "," + MD5Value3 + "," + address3 + ";" + MD5Value3 + "," + MD5Value1 + "," + address1 + ";";

        Assertions.assertEquals(ecsServer.getKeyRanges(), actual + "\r\n");
    }

    @Test
    public void removeKVServerFromRingOneElementTest() {
        String address1 = "127.0.0.1:3343";
        ecsServer.putKVServerToRing(address1);
        ecsServer.removeKVServerFromRing(address1);

        Assertions.assertEquals(ecsServer.getKeyRanges(), "\r\n");
    }

    @Test
    public void removeKVServerFromRingMultiElementTest() {
        String address1 = "127.0.0.1:3343";
        String MD5Value1 = "ce887e439e7c12bc626688186ff2c51d";
        String address2 = "127.0.0.1:3344";
        String address3 = "127.0.0.1:3345";
        String MD5Value3 = "9fe0aaac6335b1cbf0a7aecef03d0c05";
        ecsServer.putKVServerToRing(address1);
        ecsServer.putKVServerToRing(address2);
        ecsServer.putKVServerToRing(address3);
        ecsServer.removeKVServerFromRing(address2);
        String actual = MD5Value1 + "," + MD5Value3 + "," + address3 + ";" + MD5Value3 + "," + MD5Value1 + "," + address1 + ";";

        Assertions.assertEquals(ecsServer.getKeyRanges(), actual + "\r\n");
    }

    @Test
    public void addKVServerToClientConnectionsTest() {
        String address1 = "127.0.0.1:3343";
        PrintWriter out = new PrintWriter(PrintWriter.nullWriter());
        ecsServer.addKVServerToClientConnections(address1, out);

        Assertions.assertEquals(ecsServer.getKVServerStreams().get(address1), out);
    }

    @Test
    public void removeKVServerToClientConnectionsTest() {
        String address1 = "127.0.0.1:3343";
        PrintWriter out = new PrintWriter(PrintWriter.nullWriter());
        ecsServer.addKVServerToClientConnections(address1, out);
        ecsServer.removeKVServerToClientConnections(address1);

        Assertions.assertNull(ecsServer.getKVServerStreams().get(address1));
    }

    @Test
    public void removalDataTransferTest() {
        String successorAddress = "127.0.0.1:3343";
        Pair<String, String> range = new Pair<>("ce887e439e7c12bc626688186ff2c51d", "9fe0aaac6335b1cbf0a7aecef03d0c05");
        String result = ecsServer.removalDataTransfer(range, successorAddress);
        String actual = "transfer_data_kill " + successorAddress + ";" + range.getValue0() + ";" + range.getValue1() + "\r\n";

        Assertions.assertEquals(result, actual);
    }

    @Test
    public void getSuccessorAddressTest() {
        String address1 = "127.0.0.1:3343";
        String address2 = "127.0.0.1:3344";
        String address3 = "127.0.0.1:3345";
        ecsServer.putKVServerToRing(address1);
        ecsServer.putKVServerToRing(address2);
        ecsServer.putKVServerToRing(address3);

        Assertions.assertEquals(ecsServer.getSuccessorAddress(address1), address2);
    }


    @Test
    public void initializationDataTransferTest() throws IOException, InterruptedException {
        String address1 = "127.0.0.1:3343";
        String address2 = "127.0.0.1:5159";
        String address3 = "127.0.0.1:3345";
        ecsServer.putKVServerToRing(address1);
        ecsServer.putKVServerToRing(address2);
        ecsServer.putKVServerToRing(address3);
        String setWrite = "set_write_lock";
        String transfer = "transfer_data_init " + address1 + ";9fe0aaac6335b1cbf0a7aecef03d0c05;ce887e439e7c12bc626688186ff2c51d";

        final var latch = new CountDownLatch(1);
        final var latch2 = new CountDownLatch(1);
        final var count = new CountDownLatch(2);

        new Thread(() -> {
            try {
                latch.countDown();
                Socket clientServer = serverSocket.accept();
                PrintWriter out = new PrintWriter(new OutputStreamWriter(clientServer.getOutputStream(), Constants.TELNET_ENCODING));
                ecsServer.addKVServerToClientConnections(address2, out);
                latch2.countDown();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        latch.await();
        Socket connectToECS = new Socket("127.0.0.2", 5160);
        new Thread(() -> {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connectToECS.getInputStream(), Constants.TELNET_ENCODING))) {
                String firstLine;
                while ((firstLine = in.readLine()) != null) {
                    if (firstLine.equals(setWrite)) {
                        count.countDown();
                    } else if (firstLine.equals(transfer)) {
                        count.countDown();
                    }
                }
            } catch (IOException exception) {
                Assertions.fail();
            }
        }).start();


        latch2.await();
        ecsServer.initializationDataTransfer(address1);
        count.await();
        Assertions.assertTrue(true);
    }

    @Test
    public void sendMetadataToSuccessorTest() throws InterruptedException, IOException {
        String address1 = "127.0.0.1:3343";
        String MD5Value1 = "ce887e439e7c12bc626688186ff2c51d";
        String address2 = "127.0.0.1:3344";
        String MD5Value2 = "9251e2d00e6ddb1a7572e2782d5146fa";
        String address3 = "127.0.0.1:3345";
        String MD5Value3 = "9fe0aaac6335b1cbf0a7aecef03d0c05";
        ecsServer.putKVServerToRing(address1);
        ecsServer.putKVServerToRing(address2);
        ecsServer.putKVServerToRing(address3);
        String actual = "update_ranges " + MD5Value1 + "," + MD5Value2 + "," + address2 + ";" + MD5Value2 + "," + MD5Value3 + "," + address3 + ";" + MD5Value3 + "," + MD5Value1 + "," + address1 + ";";

        final var latch = new CountDownLatch(1);
        final var latch2 = new CountDownLatch(1);
        final var count = new CountDownLatch(1);

        new Thread(() -> {
            try {
                latch.countDown();
                Socket clientServer = serverSocket.accept();
                PrintWriter out = new PrintWriter(new OutputStreamWriter(clientServer.getOutputStream(), Constants.TELNET_ENCODING));
                ecsServer.addKVServerToClientConnections(address2, out);
                latch2.countDown();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        latch.await();
        Socket connectToECS = new Socket("127.0.0.2", 5160);
        new Thread(() -> {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connectToECS.getInputStream(), Constants.TELNET_ENCODING))) {
                String firstLine;
                while ((firstLine = in.readLine()) != null) {
                    if (firstLine.equals(actual)) {
                        count.countDown();
                    }
                }
            } catch (IOException exception) {
                Assertions.fail();
            }
        }).start();


        latch2.await();
        ecsServer.sendMetadataToSuccessor(address2);
        count.await();
        Assertions.assertTrue(true);
    }

    @Test
    public void sendHeartbeatsSuccessTest() throws InterruptedException, IOException {
        String address1 = "127.0.0.1:3343";
        String address2 = "127.0.0.1:5159";
        String address3 = "127.0.0.1:3345";
        ecsServer.putKVServerToRing(address1);
        ecsServer.putKVServerToRing(address2);
        ecsServer.putKVServerToRing(address3);

        final var latch = new CountDownLatch(1);
        final var latch2 = new CountDownLatch(1);

        new Thread(() -> {
            try {
                latch.countDown();
                Socket clientServer = serverSocket.accept();
                PrintWriter out = new PrintWriter(new OutputStreamWriter(clientServer.getOutputStream(), Constants.TELNET_ENCODING));
                ecsServer.addKVServerToClientConnections(address2, out);
                latch2.countDown();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        latch.await();
        new Socket("127.0.0.2", 5160);

        latch2.await();
        ecsServer.sendHeartbeats(ecsIp);
        Assertions.assertEquals(ecsServer.ringSize(), 3);
    }

    @Test
    public void sendHeartbeatsFailTest() throws InterruptedException, IOException {
        String address1 = "127.0.0.1:3343";
        String address2 = "127.0.0.1:5159";
        String address3 = "127.0.0.1:3345";
        ecsServer.putKVServerToRing(address1);
        ecsServer.putKVServerToRing(address2);
        ecsServer.putKVServerToRing(address3);

        final var latch = new CountDownLatch(1);
        final var latch2 = new CountDownLatch(1);

        new Thread(() -> {
            try {
                latch.countDown();
                Socket clientServer = serverSocket.accept();
                PrintWriter out = new PrintWriter(new OutputStreamWriter(clientServer.getOutputStream(), Constants.TELNET_ENCODING));
                ecsServer.addKVServerToClientConnections(address2, out);
                latch2.countDown();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        latch.await();
        Socket clientSocket = new Socket("127.0.0.2", 5160);

        latch2.await();
        clientSocket.close();
        ecsServer.sendHeartbeats(ecsIp);
        Thread.sleep(3000);
        Assertions.assertEquals(ecsServer.ringSize(), 2);
    }
}
