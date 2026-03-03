package bt3;

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
                System.out.println("Server console ready. Type a message and hit enter to broadcast.");
                System.out.println("Send mode to access private mode:");
                while (true) {
                    String message = br.readLine();
                    if (message.equalsIgnoreCase("stop")) {
                        broadcast("Server is shutting down...");
                        broadcast("Server Admin: " + message);
                        System.exit(0);
                    }
                    if (message.equalsIgnoreCase("mode")) {
                        System.out.println("Server ready for private mode...");
                        System.out.println("Enter Client ID:");
                        int id = Integer.parseInt(br.readLine());
                        System.out.println("Enter Message:");
                        String pvm = br.readLine();
                        privateMessage(id, pvm);
                    }
                    broadcast(message);

                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    // Send a message to every connected client
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
                // If queue is full, this will block until space is available
                queue.put(clientSocket);
                System.out.println("Client accepted: " + clientSocket);

                // Create handler and add to the broadcast list
                ClientHandler clientHandler = new ClientHandler(clientSocket, queue);
                clients.add(clientHandler);

                // Start the handler thread (which will start listening to the client)
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
