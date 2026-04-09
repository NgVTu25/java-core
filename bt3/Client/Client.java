package bt3.Client;

import bt3.ConfigReader;
import bt3.Server.FileService;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Client implements Runnable {
    private ObjectInputStream is;
    private final BlockingQueue<Object> queue;

    private Socket socket;
    private BufferedReader br;
    private ObjectOutputStream os;
    private final ConfigReader config = ConfigReader.getInstance();


    public Client() {
        this.queue = new ArrayBlockingQueue<>(Integer.parseInt(config.getConfig("num.threads")));
    }

    @Override
    public void run() {
        String ip = "localhost";
        try {
            socket = new Socket(ip, Integer.parseInt(config.getConfig("port")));
            System.out.println("Kết nối thành công tới Server: " + ip);

            os = new ObjectOutputStream(socket.getOutputStream());
            is = new ObjectInputStream(socket.getInputStream());

            br = new BufferedReader(new InputStreamReader(System.in));
            os.flush();

            Thread receiveThread = new Thread(() -> {
                try {
                    while (true) {
                        Object obj = is.readObject();
                        if (obj instanceof String msg) {
                            if (msg.startsWith("ACCEPT") || msg.startsWith("REJECT") || msg.equalsIgnoreCase("stop")) {
                                queue.put(msg);
                            } else {
                                System.out.println("\n[Tin nhắn từ Server]: " + msg);
                            }
                        } else {
                            queue.put(obj);
                        }
                    }
                } catch (Exception e) {
                    System.out.println("\n[Hệ thống] Đã ngắt luồng nhận tin nhắn.");
                    e.printStackTrace();
                }
            });
            receiveThread.start();

            while (true) {
                System.out.println("\n=== HỆ THỐNG QUẢN LÝ FILE & CHAT ===");
                System.out.println("1. Xem danh sách file trên Server (CHECK)");
                System.out.println("2. Tải file từ Server về máy (DOWNLOAD)");
                System.out.println("3. Đẩy file từ máy lên Server (UPLOAD)");
                System.out.println("4. Gửi tin nhắn lên Server (CHAT)");
                System.out.println("5. Gửi tin nhắn cho client (CHAT)");
                System.out.println("6. Thoát (STOP)");
                System.out.print("Vui lòng chọn chức năng (1-6): ");
                FileService fileService = new FileService(queue, os);

                String file;
                String choice = br.readLine().trim();
                switch (choice) {

                    case "1":
                        fileService.requestCheck();
                        break;

                    case "2":
                        System.out.print("Nhập tên file cần tải về: ");
                        file = br.readLine().trim();

                        if (file.isEmpty()) {
                            System.out.println("Tên file không được để trống!");
                            break;
                        }
                        fileService.requestDownload(file);
                        break;

                    case "3":
                        System.out.print("Nhập đường dẫn tuyệt đối của file: ");
                        file = br.readLine().trim();
                        if (file.isEmpty()) {
                            System.out.println("Đường dẫn không được để trống!");
                            break;
                        }
                        fileService.requestUpload(file);
                        break;

                    case "4":
                        fileService.chatMode();
                        break;

                    case "5":
                        System.out.println("Enter Client ID:");
                        int receiverId = Integer.parseInt(br.readLine());
                        while (true) {
                            String message = br.readLine();

                            if (message.equalsIgnoreCase("stop")) {
                                break;
                            }

                            fileService.clientToClient(receiverId, message);
                        }
                        break;

                    case "6":
                        os.writeObject("stop");
                        os.flush();
                        closeConnection();
                        System.exit(0);
                        break;

                    default:
                        System.out.println("Lựa chọn không hợp lệ!");
                }
            }
        } catch (Exception e) {
            System.out.println("Lỗi kết nối: " + e.getMessage());
        }
    }

    private void closeConnection() {
        try {
            if (br != null) br.close();
        } catch (IOException ignored) {
        }
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


    public static void main(String[] args) {
        new Client().run();
    }
}


