package bt3;

import bt3.model.FileChuck;
import bt3.Server.FileService;

import java.io.*;


public class Download extends Command {
    private final ConfigReader config = ConfigReader.getInstance();


    @Override
    public void execute(CommandRequest request, ObjectOutputStream os, ObjectInputStream is, FileService fileService) throws IOException {
        String filename = request.filename();
        long offset = request.offset();

        String filePath = config.getConfig("download.path") + "/" + filename;
        File file = new File(filePath);

        if (!file.exists()) {
            FileChuck rejectChunk = new FileChuck();
            rejectChunk.setStatus("REJECT: File không tồn tại trên server");
            os.writeObject(rejectChunk);
            os.flush();
            return;
        }


        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(offset);

            byte[] buffer = new byte[1024 * 64];
            int bytes;

            while ((bytes = raf.read(buffer)) != -1) {
                FileChuck chunk = new FileChuck();
                chunk.setFileName(filename);
                chunk.setOffset(offset);
                chunk.setStatus("OK");

                byte[] actualData = new byte[bytes];
                System.arraycopy(buffer, 0, actualData, 0, bytes);
                chunk.setData(actualData);

                offset += bytes;
                chunk.setCompleted(raf.getFilePointer() == raf.length());

                os.writeObject(chunk);
                os.flush();
            }
        }
    }
}