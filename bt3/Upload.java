package bt3;

import bt3.model.FileChuck;
import bt3.Server.FileService;

import java.io.*;

public class Upload implements Command {
    private final ConfigReader config = ConfigReader.getInstance();

    @Override
    public void execute(CommandRequest request, ObjectOutputStream os, ObjectInputStream is, FileService fileService) throws IOException {
        String filename = request.filename();
        long clientFileSize = request.offset();

        String savePath = config.getConfig("download.path") + "/" + filename;
        File file = new File(savePath);

        long currentOffset = 0;

        if (file.exists()) {
            currentOffset = file.length();
            if (currentOffset >= clientFileSize && clientFileSize > 0) {
                os.writeObject("REJECT: File đã tồn tại hoàn chỉnh trên Server");
                os.flush();
                return;
            }
        } else {
            file.getParentFile().mkdirs();
        }

        os.writeObject("ACCEPT|" + currentOffset);
        os.flush();

        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.seek(currentOffset);
            Object receivedData;

            System.out.println("Đang nhận file " + filename + " từ offset: " + currentOffset);

            while (true) {
                receivedData = is.readObject();

                if (receivedData instanceof FileChuck chunk) {

                    raf.write(chunk.getData());

                    if (chunk.getCompleted()) {
                        System.out.println(">>> Đã nhận hoàn tất file: " + filename);
                        break;
                    }
                }
                else if (receivedData instanceof String msg) {
                    if (msg.equalsIgnoreCase("CANCEL")) break;
                }
            }
        } catch (EOFException e) {
            System.out.println("Client đột ngột ngắt kết nối khi đang Upload.");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}