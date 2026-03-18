package bt3.Client;

import bt3.ConfigReader;
import bt3.Server.FileService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Client01 implements Runnable{
    private ObjectInputStream is;
    private final BlockingQueue<Object> queue;


    public Client01() {
        ConfigReader config = ConfigReader.getInstance();
        this.queue = new ArrayBlockingQueue<>(Integer.parseInt(config.getConfig("num.threads")));
    }

    @Override
    public void run() {
        ConfigReader configReader = ConfigReader.getInstance();
        String ip = "localhost";
        try {
            Socket socket = new Socket(ip, Integer.parseInt(configReader.getConfig("port")));
            System.out.println("Kết nối thành công tới Server: " + ip);

            ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
            is = new ObjectInputStream(socket.getInputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
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
                    System.out.println("\n[Hệ thống] Đã ngắt luồn nhận tin nhắn.");
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
                FileService fileService = new FileService(socket, queue, os, is);

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
                        String id = br.readLine();
                        while (true) {
                            String message = br.readLine();
                            if (message.equalsIgnoreCase("stop")) {
                                break;
                            }
                            fileService.clientToClient(Integer.parseInt(id), message);
                        }
                        break;
                    case "6":
                        os.writeObject("stop");
                        os.flush();
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



    public static void main(String[] args) {
        new Client01().run();
    }
}


