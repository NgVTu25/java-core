package bt3.Server;

import bt3.Client.ClientHandler;
import bt3.ConfigReader;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;




public class Server {

    private ServerSocket serverSocket;
    private BlockingQueue<Socket> queue;
    private ConfigReader config;
    public static List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public Server() {
        config = ConfigReader.getInstance();
        queue = new ArrayBlockingQueue<>(
                Integer.parseInt(config.getConfig("num.threads")));
        try {
            serverSocket = new ServerSocket(
                    Integer.parseInt(config.getConfig("port")),
                    Integer.parseInt(config.getConfig("clients")));
            startServer();
            acceptConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startServer() {
        new Thread(() -> {
            try {
                BufferedReader br = new BufferedReader(new  InputStreamReader(System.in));
                System.out.println("Server đã khởi động. Nhập tin nhắn và nhấn enter để gửi tới tất cả khách hàng hoặc nhập mode để nhắn riêng.");
                while (true) {

                    String message = br.readLine();
                    if (message.equalsIgnoreCase("stop")) {
                        broadcast("Server đã tắt...");
                        broadcast("Server Admin: " + message);
                        System.exit(0);
                    }

                    if (message.equalsIgnoreCase("mode")) {
                        System.out.println("Enter Client ID:");
                        int id = Integer.parseInt(br.readLine());
                        String pvm = "";
                        while (!pvm.equalsIgnoreCase("exit")) {
                        System.out.println("Server sẵn sàng...");
                        System.out.println("Nhập tin nhắn:");
                        pvm = br.readLine();
                        privateMessage(id, pvm);
                        }
                    } else {
                        broadcast(message);
                    }

                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    public static void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    public static void privateMessage(int id, String message) {
        for (ClientHandler client : clients) {
            if (client.getId() == id) {
                client.sendMessage(message);
            }
        }
    }

    public void acceptConnection() throws IOException {
        System.out.println("Server is listening...");
        while (true) {
            Socket clientSocket = serverSocket.accept();
            try {
                queue.put(clientSocket);
                System.out.println("Client accepted: " + clientSocket);
                ClientHandler clientHandler = new ClientHandler(clientSocket, serverSocket ,queue , queue.size());
                clients.add(clientHandler);

                new Thread(clientHandler).start();

            } catch (InterruptedException e) {
                System.err.println("Failed to add client to queue");
            }
        }
    }



    public static void main(String[] args) {
        new Server();
    }
}
