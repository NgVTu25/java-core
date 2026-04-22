package bt3.service.File;

import bt3.ConfigReader;
import bt3.model.FileChuck;
import bt3.model.PrivateChatMessage;
import bt3.model.TextMessage;
import bt3.service.Chat.ChatMessageEnvelope;
import bt3.service.Chat.MessageEnvelope;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static bt3.common.EventType.*;

public class FileService {
    private final ConfigReader config;
    private final BufferedReader br;
    private final ObjectOutputStream os;
    private final int myId;

    private RandomAccessFile currentDownloadFile;
    private String pendingUploadPath;

    public FileService(ObjectOutputStream os, int myId) {
        this.config = ConfigReader.getInstance();
        this.os = os;
        this.myId = myId;
        this.br = new BufferedReader(new InputStreamReader(System.in));
    }

    public void requestCheck() throws IOException {
        CommandRequest request = new CommandRequest(LIST_FILE, "", 0, myId);
        os.writeObject(new MessageEnvelope<>(LIST_FILE, request, myId, 0));
        os.flush();
        System.out.println("Đã gửi yêu cầu. Đang chờ Server phản hồi...");
    }

    public void requestDownload(String filename) throws IOException {
        File localFile = new File("client_downloads/" + filename);
        localFile.getParentFile().mkdirs();
        long offset = localFile.exists() ? localFile.length() : 0;

        CommandRequest request = new CommandRequest(FILE_DOWNLOAD, filename, offset, myId);
        os.writeObject(new MessageEnvelope<>(FILE_DOWNLOAD, request, myId, 0));
        os.flush();
        System.out.println("Đã yêu cầu tải file: " + filename);
    }

    public void requestUpload(String filePath) throws IOException {
        File localFile = new File(filePath);
        if (!localFile.exists()) {
            System.out.println("File không tồn tại trên máy của bạn!");
            return;
        }

        this.pendingUploadPath = filePath; // Lưu tạm đường dẫn để đợi Server phản hồi ACCEPT
        String filename = localFile.getName();
        long totalSize = localFile.length();

        CommandRequest request = new CommandRequest(FILE_UPLOAD, filename, totalSize, myId);
        os.writeObject(new MessageEnvelope<>(FILE_UPLOAD, request, myId, 0));
        os.flush();
        System.out.println("Đã gửi yêu cầu Upload. Đang đợi Server duyệt...");
    }


    public void processIncomingChunk(FileChuck chunk) {
        try {
            if (currentDownloadFile == null) {
                File file = new File("client_downloads/" + chunk.getFileName());
                currentDownloadFile = new RandomAccessFile(file, "rw");
                currentDownloadFile.seek(chunk.getOffset());
            }

            currentDownloadFile.write(chunk.getData());
            System.out.print("\rĐang tải... " + currentDownloadFile.getFilePointer() + " bytes");

            if (chunk.getCompleted()) {
                System.out.println("\n[Thành công] Tải file hoàn tất!");
                currentDownloadFile.close();
                currentDownloadFile = null;
            }
        } catch (IOException e) {
            System.err.println("\nLỗi ghi file: " + e.getMessage());
        }
    }

    public void handleServerAcceptForUpload(String serverMsg) {
        if (pendingUploadPath == null) return;

        long offset = Long.parseLong(serverMsg.split("\\|")[1]);
        File localFile = new File(pendingUploadPath);
        String filename = localFile.getName();
        long totalSize = localFile.length();

        new Thread(() -> {
            try (RandomAccessFile raf = new RandomAccessFile(localFile, "r")) {
                raf.seek(offset);
                byte[] buffer = new byte[1024 * 64];
                int bytesRead;
                int chunkCount = 0;

                System.out.println("Bắt đầu đẩy dữ liệu lên Server...");

                while ((bytesRead = raf.read(buffer)) != -1) {
                    FileChuck chunk = new FileChuck();
                    chunk.setFileName(filename);

                    byte[] actualData = new byte[bytesRead];
                    System.arraycopy(buffer, 0, actualData, 0, bytesRead);
                    chunk.setData(actualData);
                    chunk.setCompleted(raf.getFilePointer() == totalSize);

                    os.writeObject(new MessageEnvelope<>(FILE_CHUNK, chunk, myId, 0));
                    os.flush();

                    chunkCount++;
                    if (chunkCount % 100 == 0) os.reset(); // Dọn rác cache Stream

                    System.out.print("\rĐã gửi: " + raf.getFilePointer() + " / " + totalSize + " bytes");
                }
                System.out.println("\n[Upload Thành Công]");
                pendingUploadPath = null;
            } catch (Exception e) {
                System.out.println("\n[Lỗi Upload] Mất mạng: " + e.getMessage());
            }
        }).start();
    }


    public void chatMode() throws IOException {
        System.out.println("--- CHẾ ĐỘ NHẮN TIN (Gõ 'exit' để thoát) ---");
        while (true) {
            System.out.print("Bạn: ");
            String msg = br.readLine().trim();
            if (msg.equalsIgnoreCase("exit")) break;

            TextMessage textMessage = new TextMessage(msg, myId, 0);
            MessageEnvelope<?> envelope = new ChatMessageEnvelope(textMessage, myId);

            os.writeObject(envelope);
            os.flush();
        }
    }

    public void clientToClient(int receiverId, String message) {
        try {
            PrivateChatMessage privateChatMessage = new PrivateChatMessage(receiverId, message);
            MessageEnvelope<?> envelope = new ChatMessageEnvelope(privateChatMessage, myId);

            os.writeObject(envelope);
            os.flush();
        } catch (IOException e) {
            System.err.println("Lỗi gửi private chat: " + e.getMessage());
        }
    }

    public void requestClientIds() {
        try {
            CommandRequest request = new CommandRequest(CLIENT_LIST, "", 0, myId);
            os.writeObject(new MessageEnvelope<>(CLIENT_LIST, request, myId, 0));
            os.flush();
        } catch (IOException e) {
            System.err.println("Lỗi yêu cầu danh sách: " + e.getMessage());
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