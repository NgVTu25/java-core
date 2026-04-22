package bt3.Server;

import bt3.service.Chat.ChatMessageEnvelope;
import bt3.ConfigReader;
import bt3.service.Chat.MessageEnvelope;
import bt3.model.PrivateChatMessage;
import bt3.model.TextMessage;
import bt3.common.EventType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {

    protected static final Set<Integer> clientIds = ConcurrentHashMap.newKeySet();
    protected static List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private static BlockingQueue<Socket> queue;
    private ServerSocket serverSocket;

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

    public static List<Integer> getClientIds() {
        return clientIds.stream().toList();
    }

    public static void broadcast(String message) {
        MessageEnvelope<?> event = new MessageEnvelope<>(EventType.CHAT, message, 0, -1);
        for (ClientHandler client : clients) {
            client.sendMessage(event);
        }
    }

    public static void privateMessage(int id, String message) {
        for (ClientHandler client : clients) {
            if (client.getClientId() == id) {
                PrivateChatMessage pcm = new PrivateChatMessage(id, message);
                client.sendMessage(new ChatMessageEnvelope(pcm, 0));
                return;
            }
        }
        System.out.println("Không tìm thấy client có id = " + id);
    }

    public static void removeClient(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        clientIds.remove(clientHandler.getClientId());
        queue.poll();

        broadcast("[Hệ thống] Client id = " + clientHandler.getClientId() + " đã ngắt kết nối.");
        System.out.println("Đã xóa client id = " + clientHandler.getClientId());
    }

    static void main(String[] args) {
        new Server();
    }

    public static void forwardMessage(MessageEnvelope<?> env) {
        for (ClientHandler client : clients) {
            if (client.getClientId() != env.getReceiverId()) {
                client.sendMessage(env);
                return;
            }
        }
        privateMessage(env.getSenderId(), "Không tìm thấy Client " + env.getReceiverId());
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
                        for(ClientHandler c : clients){
                            c.sendMessage(new MessageEnvelope<>(EventType.STOP, "Server đang tắt. Kết nối sẽ bị ngắt.", 0, c.getClientId()));
                        }
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

    public void acceptConnection() throws IOException {
        System.out.println("Server is listening...");
        Random random = new Random();

        while (true) {
            Socket clientSocket = serverSocket.accept();
            int clientId;

            do {
                clientId = random.nextInt(15, 999999);
            } while (clientIds.contains(clientId));

            ClientHandler clientHandler = new ClientHandler(clientSocket, clientId);

            if (queue.remainingCapacity() == 0) {
                MessageEnvelope<?> fullMsg = new MessageEnvelope<>(EventType.SERVER_FULL, "Server đầy.", 0, clientId);
                clientHandler.sendMessage(fullMsg);
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("Đã từ chối kết nối mới (Server full).");
            } else {
                queue.add(clientSocket);
                clients.add(clientHandler);
                clientIds.add(clientId);

                MessageEnvelope<?> idMsg = new MessageEnvelope<>(EventType.ACCEPT, clientId, 0, clientId);
                clientHandler.sendMessage(idMsg);

                new Thread(clientHandler).start();

                broadcast("[Hệ thống] Client mới đã tham gia với ID: " + clientId);
            }
        }
    }
}