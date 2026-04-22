package bt3.Client;

import bt3.ConfigReader;
import bt3.common.EventType;
import bt3.model.FileChuck;
import bt3.service.Chat.MessageEnvelope;
import bt3.service.File.FileService;

import java.io.*;
import java.net.Socket;
import java.util.List;

public class ClientOne implements Runnable {
    private ObjectInputStream is;
    private ObjectOutputStream os;
    private Socket socket;
    private BufferedReader br;
    private FileService fileService;
    private int myId = -1;

    public static void main(String[] args) {
        new Thread(new ClientOne()).start();
    }

    @Override
    public void run() {
        ConfigReader config = ConfigReader.getInstance();
        try {
            socket = new Socket("localhost", Integer.parseInt(config.getConfig("port")));
            os = new ObjectOutputStream(socket.getOutputStream());
            is = new ObjectInputStream(socket.getInputStream());
            os.flush();

            new Thread(this::receiveLoop).start();
            startUserInterface();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void receiveLoop() {
        try {
            while (true) {
                Object obj = is.readObject();
                if (obj instanceof MessageEnvelope<?> env) {
                    handleNotification(env);
                }
            }
        } catch (Exception e) {
            System.out.println("\n[Hệ thống] Mất kết nối tới Server. (" + e.getMessage() + ")");
            System.exit(0);
        }
    }

    private void handleNotification(MessageEnvelope<?> env) {
        EventType type = env.getType();
        Object payload = env.getPayload();

        switch (type) {
            case CHAT, PRIVATE_CHAT -> {
                System.out.println("\n[Tin nhắn]: " + payload);
                reprintPrompt();
            }

            case FILE_CHUNK -> {
                if (fileService != null) fileService.processIncomingChunk((FileChuck) payload);
            }

            case LIST_FILE -> {
                if (payload instanceof List<?> files) {
                    System.out.println("\n--- Danh sách file trên Server ---");
                    files.forEach(f -> System.out.println(" - " + f));
                    reprintPrompt();
                }
            }

            // XỬ LÝ LỆNH LẤY DANH SÁCH ID (An toàn)
            case CLIENT_LIST -> {
                if (payload instanceof List<?> ids) {
                    System.out.println("\n\n--- DANH SÁCH ID CLIENT ĐANG HOẠT ĐỘNG ---");
                    if (ids.isEmpty() || (ids.size() == 1 && ids.contains(myId))) {
                        System.out.println("Không có ai khác ngoài bạn.");
                    } else {
                        for (Object id : ids) {
                            if ((Integer) id == myId) {
                                System.out.println(" + " + id + " (Bạn)");
                            } else {
                                System.out.println(" + " + id);
                            }
                        }
                    }
                    reprintPrompt();
                } else {
                    System.out.println("\n[Lỗi] Dữ liệu Client List không hợp lệ.");
                }
            }

            case ACCEPT -> {
                if (payload instanceof Integer id) {
                    this.myId = id;
                    this.fileService = new FileService(os, myId);
                    System.out.println("\n[Hệ thống] Kết nối thành công. ID của bạn là: " + myId);
                }
                else if (payload instanceof String msg && fileService != null) {
                    fileService.handleServerAcceptForUpload(msg);
                }
            }

            case REJECT -> {
                System.out.println("\n[Server Từ Chối]: " + payload);
                reprintPrompt();
            }

            case SERVER_FULL, STOP -> {
                System.out.println("\n[Hệ thống]: " + payload);
                System.exit(0);
            }

            default -> System.out.println("\nUnknown Event: " + type);
        }
    }

    // Hàm in lại dấu nhắc nhập liệu cho giao diện đẹp hơn
    private void reprintPrompt() {
        System.out.print("\nChọn (1-7): ");
    }

    private void startUserInterface() {
        try {
            br = new BufferedReader(new InputStreamReader(System.in));

            System.out.print("Đang chờ cấp phát ID từ Server...");
            while (myId == -1) {
                Thread.sleep(100);
            }

            while (true) {
                System.out.println("\n=== HỆ THỐNG QUẢN LÝ FILE & CHAT ===");
                System.out.println("1. Xem danh sách file (CHECK)");
                System.out.println("2. Tải file (DOWNLOAD)");
                System.out.println("3. Tải lên (UPLOAD)");
                System.out.println("4. Chat Global (CHAT)");
                System.out.println("5. Chat Private");
                System.out.println("6. Lấy danh sách ID Client");
                System.out.println("7. Thoát (STOP)");
                System.out.print("Chọn (1-7): ");

                String choice = br.readLine().trim();

                switch (choice) {
                    case "1" -> fileService.requestCheck();

                    case "2" -> {
                        System.out.print("Nhập tên file cần tải: ");
                        String filename = br.readLine().trim();
                        if (!filename.isEmpty()) fileService.requestDownload(filename);
                    }

                    case "3" -> {
                        System.out.print("Nhập đường dẫn file (VD: C:/docs/test.txt): ");
                        String filepath = br.readLine().trim();
                        if (!filepath.isEmpty()) fileService.requestUpload(filepath);
                    }

                    case "4" -> fileService.chatMode();

                    case "5" -> {
                        System.out.print("Nhập ID người nhận: ");
                        try {
                            int receiverId = Integer.parseInt(br.readLine().trim());
                            System.out.print("Nhập tin nhắn: ");
                            String msg = br.readLine().trim();
                            fileService.clientToClient(receiverId, msg);
                        } catch (NumberFormatException e) {
                            System.out.println("ID phải là số!");
                        }
                    }

                    case "6" -> {
                        System.out.println("Đang yêu cầu danh sách từ Server...");
                        fileService.requestClientIds();
                    }

                    case "7" -> {
                        os.writeObject(new MessageEnvelope<>(EventType.STOP, "Disconnect", myId, 0));
                        os.flush();
                        closeConnection();
                        System.exit(0);
                    }

                    default -> System.out.println("Không hợp lệ. Vui lòng chọn từ 1-7.");
                }
            }
        } catch (Exception e) {
            System.out.println("Lỗi giao diện: " + e.getMessage());
        }
    }

    private void closeConnection() throws IOException {
        if (socket != null && !socket.isClosed()) socket.close();
        if (is != null) is.close();
        if (os != null) os.close();
    }
}