package bt3.Server;

import bt3.service.Chat.MessageEnvelope;
import bt3.service.File.*;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import static bt3.common.EventType.*;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final int clientId;
    private ObjectOutputStream os;
    private ObjectInputStream is;
    private FileService fileService;

    public ClientHandler(Socket socket, int clientId) {
        this.socket = socket;
        this.clientId = clientId;

        try {
            this.os = new ObjectOutputStream(socket.getOutputStream());
            this.os.flush();
            this.fileService = new FileService(this.os, clientId);
        } catch (IOException e) {
            System.err.println("Lỗi khởi tạo luồng Output cho Client " + clientId);
        }
    }

    @Override
    public void run() {
        try {
            this.is = new ObjectInputStream(socket.getInputStream());

            while (true) {
                Object receivedData = is.readObject();

                if (receivedData instanceof MessageEnvelope<?> env) {

                    if (env.getType() == STOP) {
                        System.out.println("Client " + clientId + " yêu cầu ngắt kết nối.");
                        break;
                    }

                    handleMessageEnvelope(env);

                } else {
                    System.out.println("Cảnh báo: Nhận được dữ liệu không bọc trong MessageEnvelope từ Client " + clientId);
                }
            }
        } catch (EOFException e) {
            System.out.println("Kết nối với Client " + clientId + " đã đóng.");
        } catch (Exception e) {
            System.out.println("Lỗi xử lý luồng nhận từ Client " + clientId + ": " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    private void handleMessageEnvelope(MessageEnvelope<?> env) {
        int receiverId = env.getReceiverId();

        if (receiverId == -1) {
            Server.forwardMessage(env);
        }
        else if (receiverId != 0) {
            Server.forwardMessage(env);
        }
        else {
            if (env.getPayload() instanceof CommandRequest req) {
                handleCommandRequest(req);
            } else {
                System.out.println("Server nhận được payload không phải CommandRequest từ Client " + clientId);
            }
        }
    }

    private void handleCommandRequest(CommandRequest request) {
        try {
            Command command = CommandControl.getCommand(String.valueOf(request.commandType()));

            if (command != null) {
                command.execute(request, os, is, fileService);
            } else {
                System.err.println("Lệnh không hỗ trợ: " + request.commandType());
            }
        } catch (Exception e) {
            System.err.println("Lỗi thực thi lệnh từ Client " + clientId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendMessage(MessageEnvelope<?> event) {
        try {
            os.writeObject(event);
            os.flush();
            os.reset();
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
        } catch (IOException ignored) {}
    }

    public int getClientId() {
        return clientId;
    }
}