package bt3;

import java.io.*;
import java.net.Socket;

public class Client01 implements Runnable{
    private Socket socket;
    private ObjectInputStream is;
    private ObjectOutputStream os;

    public Client01() {
    }

    @Override
    public void run() {
        ConfigReader configReader = ConfigReader.getInstance();
            String ip = "localhost";
        try {
            socket = new Socket(ip, Integer.parseInt(configReader.getConfig("port")));
            System.out.println("Connected to " + ip + ":" + socket.getLocalPort());

            os = new ObjectOutputStream(socket.getOutputStream());
            os.flush();

            is = new ObjectInputStream(socket.getInputStream());

            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

            Thread receiveThread = new Thread(() -> {
                try {
                    while (true) {
                        Object receivedData = is.readObject();

                        if (receivedData instanceof String) {
                            String str = (String) receivedData;
                            if (str.equals("stop")) {
                                System.out.println("Disconnected from " + ip + ":" + socket.getLocalPort());
                                break;
                            }
                            System.out.println("Server: " + str);
                        } else if (receivedData instanceof FileInfo) {
                            FileInfo fileInfo = (FileInfo) receivedData;
                            System.out.println("file: " + fileInfo.getFilename());
                            System.out.println("Complete!");
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Server: " + e.getMessage());
                }
            });

            receiveThread.start();

            while (true) {
                String msg = br.readLine();
                if (msg.equals("stop")) {
                    os.writeObject(msg);
                    os.flush();
                    break;
                }
                os.writeObject(msg);
                os.flush();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        new Client01().run();
    }
}


