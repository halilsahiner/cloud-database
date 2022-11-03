package de.tum.i13;

public class KVIntegrationTest {
/*
    public static Integer port = 5555;

    public String doRequest(Socket s, String req) throws IOException {
        PrintWriter output = new PrintWriter(s.getOutputStream());
        BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream()));

        output.write(req + "\r\n");
        output.flush();

        return input.readLine();
    }

    public String doRequest(String req) throws IOException {
        Socket s = new Socket();
        s.connect(new InetSocketAddress("127.0.0.1", port));
        String res = doRequest(s, req);
        s.close();

        return res;
    }

    @Test
    public void smokeTest() throws InterruptedException {
        Thread th = new Thread(() -> {
            try {
                Main.main(new String[]{});
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        th.start(); // started the server
        Thread.sleep(2000);

        new Thread(() -> {
            try {
                Socket s = new Socket();
                s.connect(new InetSocketAddress("127.0.0.1", port));
                String command = "hello ";
                assertThat(doRequest(command), is(equalTo(command)));
                s.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        Thread.sleep(5000);
        th.interrupt();

    }

    @Test
    public void enjoyTheEcho() throws InterruptedException {
        Thread th = new Thread(() -> {
            try {
                Main.main(new String[]{});
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        th.start(); // started the server
        Thread.sleep(2000);

        for (int tcnt = 0; tcnt < 2; tcnt++) {
            final int finalTcnt = tcnt;
            new Thread(() -> {
                try {
                    Thread.sleep(finalTcnt * 100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    for (int i = 0; i < 100; i++) {
                        Socket s = new Socket();
                        s.connect(new InetSocketAddress("127.0.0.1", port));
                        String command = "hello " + finalTcnt;
                        assertThat(doRequest(command), is(equalTo(command)));
                        s.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }

        Thread.sleep(5000);
        th.interrupt();
    }*/
}
