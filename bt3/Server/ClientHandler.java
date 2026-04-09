package bt3.Server;

import bt3.*;
import bt3.model.PrivateChatMessage;
import bt3.model.TextMessage;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final int clientId;
    private BlockingQueue<Object> queue;
    private ObjectOutputStream os;
    private ObjectInputStream is;

    public ClientHandler(Socket socket, int clientId) {
        this.socket = socket;
        this.clientId = clientId;
        try {
            this.os = new ObjectOutputStream(socket.getOutputStream());
            this.os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void run() {
        try {
            is = new ObjectInputStream(socket.getInputStream());
            FileService fileService = new FileService(queue, os);

            while (true) {
                try {
                    Object receivedData = is.readObject();

                    if (receivedData instanceof CommandRequest request) {
                        handleCommandRequest(request, fileService);
                    } else if (receivedData instanceof MessageEnvelope env) {
                        handleMessageEnvelope(env);
                    } else {
                        if (receivedData instanceof String str) {
                            if (str.equalsIgnoreCase("stop")) {
                                System.out.println("Client yêu cầu ngắt kết nối.");
                                break;
                            }
                        }

                        queue.put(receivedData);
                    }
                } catch (Exception e) {
                    System.out.println("Lỗi xử lý luồng nhận: " + e.getMessage());
                    break;
                }
            }
        } catch (EOFException e) {
            System.out.println("Kết nối với Server đã đóng.");
        } catch (Exception e) {
            System.out.println("Lỗi bất ngờ: " + e.getMessage());
            e.printStackTrace();
        } finally {
            disconnect();
        }
    }

    private void handleMessageEnvelope(MessageEnvelope env) {
        switch (env.type()) {
            case "CHAT" -> {
                TextMessage textMessage = (TextMessage) env.payload();
                System.out.println("\n[Global]" + "ClientID: " + clientId + "\n" + textMessage.content());
                System.out.print("Bạn: ");
            }

            case "PRIVATE_CHAT" -> {
                PrivateChatMessage message = (PrivateChatMessage) env.payload();
                Server.privateMessage(message.receiverId(), "[Client " + clientId + "]: " + message.content());
            }

            case "GET_ID" -> {
                List<Integer> clientIds = Server.getClientIds();
                System.out.println("\n[Client " + clientId + "] Yêu cầu danh sách client IDs: " + clientIds);
                Server.privateMessage(clientId, "Danh sách client IDs: " + clientIds);
            }
            default -> System.out.println("\n[Hệ thống] Nhận loại tin nhắn lạ: " + env.type());
        }
    }

    private void handleCommandRequest(CommandRequest request, FileService fileService) {
        try {
            Command command = CommandControl.getCommand(request.commandType());
            if (command != null) {
                command.execute(request, os, is, fileService);
            } else {
                System.err.println("Lệnh không hỗ trợ: " + request.commandType());
            }
        } catch (Exception e) {
            System.err.println("Lỗi thực thi lệnh " + request.commandType() + ": " + e.getMessage());
        }
    }

    public void sendMessage(String msg) {
        try {
            os.writeObject(msg);
            os.flush();
        } catch (IOException e) {
            System.out.println("Lỗi gửi message tới client " + clientId + ": " + e.getMessage());
        }
    }

    private void disconnect() {
        Server.removeClient(this);

        try {
            if (is != null) is.close();
        } catch (IOException ignored) {
        }
        try {
            if (os != null) os.close();
        } catch (IOException ignored) {
        }
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {
        }
    }

    public int getClientId() {
        return clientId;
    }

}