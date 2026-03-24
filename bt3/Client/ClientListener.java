package bt3.Client;

import bt3.*;
import bt3.Server.FileService;
import bt3.Server.Server;
import bt3.model.PrivateChatMessage;
import bt3.model.TextMessage;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

public class ClientListener implements Runnable {
    private final Socket socket;
    public final BlockingQueue<Socket> queue;
    private ObjectOutputStream os;
    private ObjectInputStream is;
    private int id;

    public ClientListener(Socket socket, BlockingQueue<Socket> queue) {
        this.socket = socket;
        this.queue = queue;
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
            FileService fileService = new FileService(os);

            while (true) {
                try {
                    Object receivedData = is.readObject();

                    if (receivedData instanceof CommandRequest request) {
                        Command command = CommandControl.getCommand(request.commandType());

                        if (command != null) {
                            command.execute(request, os, is, fileService);
                        }

                    } else if (receivedData instanceof MessageEnvelope env) {
                        handleMessageEnvelope(env);

                    } else {
                        System.out.println("Dữ liệu không hỗ trợ: " + receivedData.getClass().getName());
                    }

                } catch (Exception e) {
                    System.out.println("Lỗi xử lý yêu cầu: " + e.getMessage());
                    break;
                }
            }

        } catch (EOFException e) {
            System.out.println("Client disconnected");
        } catch (Exception e) {
            System.out.println("Client disconnected unexpectedly: " + socket.getRemoteSocketAddress());
            e.printStackTrace();
        } finally {
            disconnect();
        }
    }

    private void handleMessageEnvelope(MessageEnvelope env) {
        String type = env.type();

        switch (type) {
            case "CHAT":
                if (env.payload() instanceof TextMessage tm) {
                    System.out.println("[Client " + tm.sender() + " nói]: " + tm.content());
                }
                break;

            case "PRIVATE_CHAT":
                if (env.payload() instanceof PrivateChatMessage(int receiverId, String content)) {
                    Server.privateMessage(receiverId, "[Từ Client " + id + "]: " + content);
                }
                break;

            default:
                System.out.println("Loại message không hỗ trợ: " + type);
        }
    }


    public void sendMessage(String msg) {
        try {
            os.writeObject(msg);
            os.flush();
        } catch (IOException e) {
            System.err.println("Lỗi khi gửi tin nhắn tới client: " + e.getMessage());
        }
    }


    private void disconnect() {
        System.out.println("Client đã ngắt kết nối: " + socket.getRemoteSocketAddress());
        try {
            if (is != null) is.close();
        } catch (IOException ignored) {
        }
        try {
            if (os != null) os.close();
        } catch (IOException ignored) {
        }
        try {
            if (!socket.isClosed()) socket.close();
        } catch (IOException ignored) {
        }
    }

    public int getId() {
        return id;
    }
}