package bt3;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

public class ClientHandler implements Runnable {
    private Socket socket;
    private ServerSocket server;
    private BlockingQueue<Socket> queue;
    private ObjectOutputStream os;
    private ObjectInputStream is;
    private ConfigReader config;
    private int id;

    public ClientHandler(Socket socket, ServerSocket server, BlockingQueue<Socket> queue, int id) {
        this.socket = socket;
        this.server = server;
        this.queue = queue;
        this.id = id;
        try {
            this.os = new ObjectOutputStream(socket.getOutputStream());
            this.os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        config = ConfigReader.getInstance();
    }

    @Override
    public void run() {
        try {
            is = new ObjectInputStream(socket.getInputStream());
            FileService fileService = new FileService(socket, server, os, is);
            while (true) {
                Object receivedData = is.readObject();
                if (receivedData instanceof String) {
                    String str = (String) receivedData;
                    if (str.equalsIgnoreCase("stop")) {
                        System.out.println("Client yêu cầu ngắt kết nối.");
                        break;
                    }
                    else if (str.equalsIgnoreCase("CHECK")) {
                       String Data = fileService.handleCheckCommand();
                       sendMessage(Data);
                    }
                    else if (str.startsWith("DOWNLOAD:")) {
                        String sourceFilePath = str.substring(9).trim();
                        System.out.println("Client yêu cầu tải file: " + sourceFilePath);
                        FileInfo fileInfo = fileService.sendFile(sourceFilePath);
                        fileService.createFiles(fileInfo);
                    }
                    else {
                        System.out.println("Client (" + socket.getRemoteSocketAddress() + "): " + str);
                    }
                }
            }
        } catch (EOFException e) {
            System.out.println("Client đã ngắt kết nối an toàn.");
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
        try {
            System.out.println("Closing connection for: " + socket.getRemoteSocketAddress());
            // Đảm bảo class Server của bạn có List tĩnh tên là clients
            // Server.clients.remove(this);
            queue.remove(socket);
            if (is != null) is.close();
            if (os != null) os.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
}