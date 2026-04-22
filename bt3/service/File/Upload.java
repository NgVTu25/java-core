package bt3.service.File;

import bt3.ConfigReader;
import bt3.common.EventType;
import bt3.model.FileChuck;
import bt3.service.Chat.MessageEnvelope;

import java.io.*;

public class Upload extends Command {
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
                // Bọc lệnh REJECT
                MessageEnvelope<?> rejectEnv = new MessageEnvelope<>(
                        EventType.REJECT,
                        "File đã tồn tại hoàn chỉnh trên Server",
                        0,
                        request.senderId()
                );
                os.writeObject(rejectEnv);
                os.flush();
                return;
            }
        } else {
            file.getParentFile().mkdirs();
        }

        MessageEnvelope<?> acceptEnv = new MessageEnvelope<>(
                EventType.ACCEPT,
                "ACCEPT|" + currentOffset,
                0,
                request.senderId()
        );
        os.writeObject(acceptEnv);
        os.flush();

        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.seek(currentOffset);
            Object receivedData;

            System.out.println("Đang nhận file " + filename + " từ offset: " + currentOffset);

            while (true) {
                receivedData = is.readObject();

                if (receivedData instanceof MessageEnvelope<?> env) {
                    if (env.getType() == EventType.FILE_CHUNK && env.getPayload() instanceof FileChuck chunk) {
                        raf.write(chunk.getData());

                        if (chunk.getCompleted()) {
                            System.out.println(">>> Đã nhận hoàn tất file: " + filename + " từ Client " + request.senderId());
                            break;
                        }
                    } else if (env.getPayload() instanceof String msg && msg.equalsIgnoreCase("CANCEL")) {
                        break;
                    }
                }
            }
        } catch (EOFException e) {
            System.out.println("Client đột ngột ngắt kết nối khi đang Upload.");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}