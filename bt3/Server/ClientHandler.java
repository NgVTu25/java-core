package bt3.Server;

import bt3.*;
import bt3.common.EventType;
import bt3.model.PrivateChatMessage;
import bt3.model.TextMessage;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import static bt3.common.EventType.*;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final int clientId;

    private final BlockingQueue<Socket> queue;
    private static BlockingQueue<MessageEnvelope> messageQueue;

    private ObjectOutputStream os;
    private ObjectInputStream is;

    public ClientHandler(Socket socket, int clientId, BlockingQueue<Socket> queue) {
        this.socket = socket;
        this.clientId = clientId;
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
            FileService fileService = new FileService(queue, os);

            while (true) {
                try {
                    Object receivedData = is.readObject();

                    if (receivedData instanceof CommandRequest request) {
                        handleCommandRequest(request, fileService);
                    } else if (receivedData instanceof MessageEnvelope env) {

                        if (env.type() == STOP) {
                            System.out.println("Client yêu cầu ngắt kết nối.");
                            break;
                        }
                        handleMessageEnvelope(env);
                    }
                    else {
                        messageQueue.put(new MessageEnvelope(EventType.CHAT, receivedData));
                    }

                } catch (Exception e) {
                    System.out.println("Lỗi xử lý luồng nhận: " + e.getMessage());
                    break;
                }
            }
        }
        catch (EOFException e) {
            System.out.println(
                    "Kết nối với client đã đóng."
            );
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            disconnect();
        }
    }


    private void handleMessageEnvelope(MessageEnvelope env) {

        switch (env.type()) {

            case CHAT -> {
                TextMessage textMessage = (TextMessage) env.payload();
                System.out.println("\n[Global] Client " + clientId + ": " + textMessage.content());
            }


            case PRIVATE_CHAT -> {
                PrivateChatMessage message = (PrivateChatMessage) env.payload();
                Server.privateMessage(message.receiverId(), "[Client " + clientId + "]: " + message.content()
                );
            }


            case CLIENT_LIST -> {
                List<Integer> clientIds = Server.getClientIds();
                Server.privateMessage(clientId, "Danh sách client IDs: " + clientIds);
            }


            case ACCEPT -> System.out.println("Upload được chấp nhận");


            case REJECT -> System.out.println("Upload bị từ chối");


            case FILE_UPLOAD -> System.out.println("Nhận event upload file");

            case FILE_DOWNLOAD -> System.out.println("Nhận event download file");

            default -> System.out.println("Unknown event: " + env.type());
        }
    }


    private void handleCommandRequest(CommandRequest request, FileService fileService) {
        try {
            Command command = CommandControl.getCommand(
                            request.commandType()
                    );
            if (command != null) {
                command.execute(request, os, is, fileService);
            } else {
                System.err.println("Lệnh không hỗ trợ: " + request.commandType());
            }
        } catch (Exception e) {
            System.err.println("Lỗi thực thi lệnh: " + e.getMessage());
        }
    }

    public void sendMessage(MessageEnvelope event) {
        try {
            os.writeObject(event);
            os.flush();
        } catch (IOException e) {
            System.out.println("Lỗi gửi tới client " + clientId);
        }
    }


    private void disconnect() {
        Server.removeClient(this);

        try {
            if (is != null) is.close();
            if (os != null) os.close();
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }

    public int getClientId() {
        return clientId;
    }
}