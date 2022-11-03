package de.tum.i13;

public class ClientCommandTests {/*
    @BeforeAll
    static void initializeServer() {

        try {
            Runtime.getRuntime().exec("docker-compose stop").waitFor();
            Runtime.getRuntime().exec("docker-compose rm -f").waitFor();
            Runtime.getRuntime().exec("docker-compose up -d ecs-server kv-server1").waitFor();
            Thread.sleep(500);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @AfterAll
    static void killServer() {
        try {
            Runtime.getRuntime().exec("docker-compose stop").waitFor();
            Runtime.getRuntime().exec("docker-compose rm -f").waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void putTest() {
        String filePrefix = "putTest";
        testHelper(filePrefix);
    }

    @Test
    public void getTest() {
        String filePrefix = "getTest";
        testHelper(filePrefix);
    }

    @Test
    public void putUpdateTest() {
        String filePrefix = "putUpdateTest";
        testHelper(filePrefix);
    }


    @Test
    public void getErrorTest() {
        String filePrefix = "getErrorTest";
        testHelper(filePrefix);
    }

    private void testHelper(String filePrefix) {
        try {
            final InputStream originalIn = System.in;
            final PrintStream originalOut = System.out;
            final FileInputStream inputStream = new FileInputStream("src/test/resources/" + filePrefix + ".txt");
            final PrintStream printStream = new PrintStream("src/test/resources/" + filePrefix + "Output.txt");


            System.setIn(inputStream);
            System.setOut(printStream);
            CommandLineInterface cli = new CommandLineInterface();
            cli.run();
            System.setIn(originalIn);
            System.setOut(originalOut);

            inputStream.close();
            printStream.close();

            FileInputStream outputStream = new FileInputStream(Paths.get("src/test/resources/" + filePrefix + "Output.txt").toFile());
            FileInputStream outputExpectedStream = new FileInputStream(Paths.get("src/test/resources/" + filePrefix + "OutputExpected.txt").toFile());
            try (BufferedInputStream fis1 = new BufferedInputStream(outputStream);
                 BufferedInputStream fis2 = new BufferedInputStream(outputExpectedStream)) {

                int ch;
                while ((ch = fis1.read()) != -1) {
                    assertEquals(ch, fis2.read());
                }
                assertEquals(-1, fis2.read());
            }
            outputStream.close();
            outputExpectedStream.close();
        } catch (IOException e) {
            fail();
        }
    }*/
}
