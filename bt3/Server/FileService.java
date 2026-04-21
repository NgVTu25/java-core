package bt3.Server;

import bt3.CommandRequest;
import bt3.ConfigReader;
import bt3.MessageEnvelope;
import bt3.model.FileChuck;
import bt3.model.PrivateChatMessage;
import bt3.model.TextMessage;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import static bt3.common.EventType.*;

public class FileService {
    private final ConfigReader config;
    private final BufferedReader br;
    private final ObjectOutputStream os;
    private final BlockingQueue<Socket> queue;

    public FileService(BlockingQueue<Socket> queue, ObjectOutputStream os) {
        this.config = ConfigReader.getInstance();
        this.os = os;
        this.br = new BufferedReader(new InputStreamReader(System.in));
        this.queue = queue;
    }

    public void requestCheck() throws IOException {
        CommandRequest request = new CommandRequest(LIST_FILE, "", 0);
        os.writeObject(request);
        os.flush();

        try {
            Object response = queue.take();
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

            CommandRequest request = new CommandRequest(FILE_DOWNLOAD, filename, offset);
            os.writeObject(request);
            os.flush();

            Object response = queue.take();
            if (response instanceof FileChuck chuck) {
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

                if (response instanceof FileChuck chunk) {
                    if (chunk.getData() != null) {
                        raf.write(chunk.getData());
                        if (chunk.getCompleted()) {
                            System.out.println("[Thành công] Tải file hoàn tất!");
                            return;
                        }
                    }
                }

                int chunkCount = 0;

                while (true) {
                    Object data = queue.take();
                    if (data instanceof FileChuck file) {
                        raf.write(file.getData());
                        System.out.print("\rĐang tải... " + raf.getFilePointer() + " bytes");

                        chunkCount++;
                        if (chunkCount % 100 == 0) {
                            os.reset();
                        }

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

            CommandRequest request = new CommandRequest(FILE_UPLOAD, filename, totalSize);
            os.writeObject(request);
            os.flush();

            Object response = queue.take();
            if (response instanceof String serverMsg) {

                if (serverMsg.startsWith("REJECT")) {
                    System.out.println("Server từ chối: " + serverMsg);
                } else if (serverMsg.startsWith("ACCEPT")) {
                    long offset = Long.parseLong(serverMsg.split("\\|")[1]);

                    try (RandomAccessFile raf = new RandomAccessFile(localFile, "r")) {
                        raf.seek(offset);
                        byte[] buffer = new byte[1024 * 64];
                        int bytesRead;

                        System.out.println("Bắt đầu Upload...");
                        int chunkCount = 0;
                        while ((bytesRead = raf.read(buffer)) != -1) {
                            FileChuck chunk = new FileChuck();
                            chunk.setFileName(filename);

                            byte[] actualData = new byte[bytesRead];
                            System.arraycopy(buffer, 0, actualData, 0, bytesRead);
                            chunk.setData(actualData);

                            chunk.setCompleted(raf.getFilePointer() == totalSize);

                            os.writeObject(chunk);
                            os.flush();
                            chunkCount++;
                            if (chunkCount % 100 == 0) {
                                os.reset();
                            }
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
                TextMessage textMessage = new TextMessage(msg);
                MessageEnvelope envelope = new MessageEnvelope(CHAT, textMessage);

                os.writeObject(envelope);
                os.flush();
            } catch (IOException e) {
                System.out.println("Lỗi gửi tin nhắn.");
            }
        }
    }


    public void clientToClient(int receiverId, String message) {

        try {

            PrivateChatMessage privateChatMessage =
                    new PrivateChatMessage(receiverId, message);

            MessageEnvelope envelope =
                    new MessageEnvelope(PRIVATE_CHAT, privateChatMessage);

            os.writeObject(envelope);
            os.flush();

        } catch (IOException e) {
            System.err.println("Lỗi gửi private chat: " + e.getMessage());
        }
    }

    public void requestClientIds() {
        try {
            MessageEnvelope envelope = new MessageEnvelope(CLIENT_LIST, new PrivateChatMessage(0, ""));
            os.writeObject(envelope);
            os.flush();
        } catch (IOException e) {
            System.err.println("Lỗi yêu cầu danh sách client IDs: " + e.getMessage());
        }
    }

    public List<String> handleCheckCommand() {
        List<String> list = new ArrayList<>();
        File folder = new File(config.getConfig("download.path"));
        if (folder.isDirectory()) {
            String[] files = folder.list();
            if (files != null) {
                list.addAll(Arrays.asList(files));
            }
        }
        return list;
    }
}
