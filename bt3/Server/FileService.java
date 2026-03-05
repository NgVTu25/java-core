package bt3.Server;

import bt3.CommandRequest;
import bt3.ConfigReader;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;


public class FileService {
    private ConfigReader config;
    private Socket client;
    private BufferedReader br;
    private ObjectOutputStream os;
    private ObjectInputStream is;
    private BlockingQueue<Object> queue;

    public FileService(Socket client, BlockingQueue<Object> queue, ObjectOutputStream os, ObjectInputStream is) {
        this.config = ConfigReader.getInstance();
        this.client = client;
        this.os = os;
        this.br = new BufferedReader(new InputStreamReader(System.in));
        this.queue = queue;
    }

    public FileService(Socket client, ObjectOutputStream os, ObjectInputStream is) {
        this.config = ConfigReader.getInstance();
        this.client = client;
        this.os = os;
        this.br = new BufferedReader(new InputStreamReader(System.in));
    }

    public FileService() {
        this.config = ConfigReader.getInstance();
        this.client = null;
        this.os = null;
        this.is = null;
    }

    public void requestCheck() throws IOException, ClassNotFoundException {
        CommandRequest request = new CommandRequest("CHECK", "", 0);
        os.writeObject(request);
        os.flush();

        try {
            Object response =queue.take();
            if (response instanceof List<?>) {
                List<String> files = (List<String>) response;
                System.out.println("\n--- Danh sách file trên Server ---");
                if (files.isEmpty()) {
                    System.out.println("(Thư mục trống)");
                } else {
                    for (String f : files) System.out.println(" - " + f);
                }
            } else if (response instanceof String) {
                System.out.println("\n" + response);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void requestDownload(String filename) {
        try {
            File localFile = new File("client_downloads/" + filename);
            long offset = localFile.exists() ? localFile.length() : 0;

            CommandRequest request = new CommandRequest("DOWNLOAD", filename, offset);
            os.writeObject(request);
            os.flush();

            Object response = queue.take();
            if (response instanceof FileChuck) {
                FileChuck chuck = (FileChuck) response;
                if (chuck.getStatus() != null && chuck.getStatus().startsWith("REJECT")) {
                    System.out.println("Server: " + chuck.getStatus());
                    return;
                }
            } else if (response instanceof String && ((String) response).startsWith("REJECT")) {
                System.out.println("Server: " + response);
                return;
            }

            System.out.println("Bắt đầu tải file...");
            try (RandomAccessFile raf = new RandomAccessFile(localFile, "rw")) {
                raf.seek(offset);

                if (response instanceof FileChuck) {
                    FileChuck chunk = (FileChuck) response;
                    if (chunk.getData() != null) {
                        raf.write(chunk.getData());
                        if (chunk.getCompleted()) {
                            System.out.println("[Thành công] Tải file hoàn tất!");
                            return;
                        }
                    }
                }

                while (true) {
                    Object data = queue.take();
                    if (data instanceof FileChuck) {
                        FileChuck file = (FileChuck) data;
                        raf.write(file.getData());
                        System.out.print("\rĐang tải... " + raf.getFilePointer() + " bytes");

                        if (file.getCompleted()) {
                            System.out.println("\n[Thành công] Tải file hoàn tất!");
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("\n[Lỗi] Mất mạng khi đang tải. Lần sau tải lại sẽ Resume.");
        }
    }

    public void requestUpload(String filePath) {
        try {
            File localFile = new File(filePath);
            if (!localFile.exists()) {
                System.out.println("File không tồn tại trên máy của bạn!");
                return;
            }

            String filename = localFile.getName();
            long totalSize = localFile.length();

            CommandRequest request = new CommandRequest("UPLOAD", filename, totalSize);
            os.writeObject(request);
            os.flush();

            Object response = queue.take();
            if (response instanceof String) {
                String serverMsg = (String) response;

                if (serverMsg.startsWith("REJECT")) {
                    System.out.println("Server từ chối: " + serverMsg);
                } else if (serverMsg.startsWith("ACCEPT")) {
                    long offset = Long.parseLong(serverMsg.split("\\|")[1]);

                    try (RandomAccessFile raf = new RandomAccessFile(localFile, "r")) {
                        raf.seek(offset);
                        byte[] buffer = new byte[1024 * 4];
                        int bytesRead;

                        System.out.println("Bắt đầu Upload...");
                        while ((bytesRead = raf.read(buffer)) != -1) {
                            FileChuck chunk = new FileChuck();
                            chunk.setFileName(filename);

                            byte[] actualData = new byte[bytesRead];
                            System.arraycopy(buffer, 0, actualData, 0, bytesRead);
                            chunk.setData(actualData);

                            chunk.setCompleted(raf.getFilePointer() == totalSize);

                            os.writeObject(chunk);
                            os.flush();
                            System.out.print("\rĐã gửi: " + raf.getFilePointer() + " / " + totalSize + " bytes");
                        }
                        System.out.println("\n[Upload Thành Công]");
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("\n[Lỗi Upload] Mất mạng. Lần sau tải lại sẽ tự động Resume.");
        }
    }

    public void chatMode() {
        System.out.println("--- CHẾ ĐỘ NHẮN TIN (Gõ 'exit' để quay lại Menu) ---");
        while (true) {
            System.out.print("Bạn: ");
            try {
                String msg = br.readLine().trim();
                if (msg.equalsIgnoreCase("exit")) break;
                os.writeObject(msg);
                os.flush();
            } catch (IOException e) {
                System.out.println("Lỗi gửi tin nhắn.");
            }
        }
    }


    public List<String> handleCheckCommand() {
       List<String> list = new ArrayList<String>();
        File folder = new File(config.getConfig("download.path"));
        if (folder.isDirectory()) {
            String[] files = folder.list();
            if (files != null && files.length > 0) {
                for (String name : files) {
                    list.add(name);
                }
            }
        }
        return list;
    }
}
