package bt3.Server;

import bt3.ConfigReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {

    private ServerSocket serverSocket;
    private final BlockingQueue<Socket> queue;
    public static List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(1);

    public Server() {
        ConfigReader config = ConfigReader.getInstance();
        queue = new ArrayBlockingQueue<>(Integer.parseInt(config.getConfig("num.threads")));

        try {
            serverSocket = new ServerSocket(
                    Integer.parseInt(config.getConfig("port")),
                    Integer.parseInt(config.getConfig("clients"))
            );

            startServer();
            acceptConnection();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startServer() {
        new Thread(() -> {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

                System.out.println("Server đã khởi động. Nhập tin nhắn để broadcast hoặc gõ mode để nhắn riêng.");

                while (true) {
                    String message = br.readLine();

                    if (message == null) break;

                    if (message.equalsIgnoreCase("stop")) {
                        broadcast("Server đã tắt...");
                        System.exit(0);
                    }

                    if (message.equalsIgnoreCase("mode")) {
                        System.out.println("Enter Client ID:");
                        int targetId = Integer.parseInt(br.readLine());

                        String pvm = "";
                        while (!pvm.equalsIgnoreCase("exit")) {
                            System.out.println("Nhập tin nhắn:");
                            pvm = br.readLine();
                            if (!pvm.equalsIgnoreCase("exit")) {
                                privateMessage(targetId, "[Server]: " + pvm);
                            }
                        }
                    } else {
                        broadcast("[Server]: " + message);
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
            if (client.getClientId() == id) {
                client.sendMessage(message);
                return;
            }
        }
        System.out.println("Không tìm thấy client có id = " + id);
    }

    public void acceptConnection() throws IOException {
        System.out.println("Server is listening...");

        while (true) {
            Socket clientSocket = serverSocket.accept();
            try {
                queue.put(clientSocket);
                int clientId = ID_GENERATOR.getAndIncrement();

                System.out.println("Client accepted: " + clientSocket + " | id = " + clientId);

                ClientHandler clientHandler = new ClientHandler(clientSocket, clientId);
                clients.add(clientHandler);

                new Thread(clientHandler).start();

            } catch (InterruptedException e) {
                System.err.println("Failed to add client to queue");
            }
        }
    }
    

    public static void removeClient(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        System.out.println("Đã xóa client id = " + clientHandler.getClientId());
    }

    public static void main(String[] args) {
        new Server();
    }
}