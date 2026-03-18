package bt3.Client;

import bt3.Command;
import bt3.CommandControl;
import bt3.CommandRequest;
import bt3.ConfigReader;
import bt3.Server.FileService;
import bt3.Server.Server;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

public class ClientHandler implements Runnable {
    private final Socket socket;
    public final BlockingQueue<Socket> queue;
    private ObjectOutputStream os;
    private ObjectInputStream is;
    private final int id;

    public ClientHandler(Socket socket, BlockingQueue<Socket> queue, int id) {
        this.socket = socket;
        this.queue = queue;
        this.id = id;
        try {
            this.os = new ObjectOutputStream(socket.getOutputStream());
            this.os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        ConfigReader config = ConfigReader.getInstance();
    }

    @Override
    public void run() {
        try {
            is = new ObjectInputStream(socket.getInputStream());
            FileService fileService = new FileService(socket, os, is);
            while (true) {
                try {
                    Object receivedData = is.readObject();
                    if (receivedData instanceof CommandRequest request) {
                        Command command = CommandControl.getCommand(request.getCommandType());

                        if (command != null) {
                            command.execute(request, os, is, fileService);
                        }
                    } else if (receivedData instanceof String str) {
                        if (str.startsWith("PRIVATE_CHAT:")) {
                            String[] parts = str.split(":", 3);
                            int receiverId = Integer.parseInt(parts[1]);
                            String chatMsg = parts[2];

                            Server.privateMessage(receiverId, "[Từ Client " + this.id + "]: " + chatMsg);
                        } else if (str.equalsIgnoreCase("stop")) {
                            System.out.println("Client " + socket.getRemoteSocketAddress() + " yêu cầu ngắt kết nối.");
                            break;
                        } else {
                            System.out.println("[Client " + getId() + socket.getRemoteSocketAddress() + " nói]: " + str);
                        }
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
        try { if (is != null) is.close(); } catch (IOException e) {}
        try { if (os != null) os.close(); } catch (IOException e) {}
        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (IOException e) {}
    }

    public int getId() { return id; }
}