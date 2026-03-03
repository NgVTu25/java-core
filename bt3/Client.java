package bt3;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Random;

public class Client implements Runnable{
    private Socket socket;
    private DataInputStream is;
    private DataOutputStream os;

    public Client() {
    }

    @Override
    public void run() {
        ConfigReader configReader = ConfigReader.getInstance();
        try {
            String ip = "localhost";
            socket = new Socket(ip, Integer.parseInt(configReader.getConfig("port")));
            System.out.println("Connected to " + ip + ":" + socket.getLocalPort());
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            is = new DataInputStream(socket.getInputStream());
            os = new DataOutputStream(socket.getOutputStream());

            Thread receiveThread = new Thread(() -> {
                    while (true) {
                        String str = null;
                        try {
                            str = is.readUTF();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        if (str.equals("stop")) {
                            break;
                        }
                        System.out.println("Server: " + str);
                    }
            });

            receiveThread.start();

            while (true) {
                String msg = br.readLine();
                if (msg.equals("stop")) {
                    break;
                }
                os.writeUTF(msg);
                os.flush();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        new Client().run();
    }
}


