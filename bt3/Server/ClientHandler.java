package bt3.Server;

import bt3.MessageEnvelope;
import bt3.model.PrivateChatMessage;
import bt3.model.TextMessage;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final int clientId;
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

            sendEnvelope(new MessageEnvelope("ASSIGN_ID", clientId));
            sendMessage("Kết nối thành công. ID của bạn là: " + clientId);

            while (true) {
                Object receivedData = is.readObject();

                if (receivedData instanceof MessageEnvelope env) {
                    handleMessageEnvelope(env);
                } else {
                    System.out.println("Dữ liệu không hỗ trợ từ client " + clientId);
                }
            }

        } catch (EOFException e) {
            System.out.println("Client disconnected: " + clientId);
        } catch (Exception e) {
            System.out.println("Lỗi client " + clientId + ": " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    private void handleMessageEnvelope(MessageEnvelope env) {
        switch (env.type()) {
            case "CHAT" -> {
                if (env.payload() instanceof TextMessage tm) {
                    String msg = "[Client " + clientId + "]: " + tm.content();
                    System.out.println(msg);
                    Server.broadcast(msg);
                }
            }
            case "PRIVATE_CHAT" -> {
                if (env.payload() instanceof PrivateChatMessage pm) {
                    int receiverId = pm.receiverId();
                    String content = pm.content();
                    Server.privateMessage(receiverId, "[Private từ " + clientId + "]: " + content);
                }
            }
            case "UPLOAD" -> {
                System.out.println("Client " + clientId + " upload: " + env.payload());
            }
            default -> System.out.println("Type không hỗ trợ: " + env.type());
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

    public void sendEnvelope(MessageEnvelope env) {
        try {
            os.writeObject(env);
            os.flush();
        } catch (IOException e) {
            System.out.println("Lỗi gửi envelope tới client " + clientId + ": " + e.getMessage());
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