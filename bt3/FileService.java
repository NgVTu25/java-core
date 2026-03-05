package bt3;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;


public class FileService {
    private ConfigReader config;
    private Socket client;
    private ServerSocket server;


    private ObjectOutputStream os;
    private ObjectInputStream is;

    public FileService(Socket client, ServerSocket server, ObjectOutputStream os, ObjectInputStream is) {
        this.config = ConfigReader.getInstance();
        this.client = client;
        this.server = server;
        this.os = os;
        this.is = is;
    }

    public boolean createFiles(FileInfo fileInfo) {
        if (fileInfo == null || fileInfo.getDataBytes() == null) return false;
        try {
            String savePath = config.getConfig("download.path") + "/" + fileInfo.getFilename();
            File fileReceive = new File(savePath);

            fileReceive.getParentFile().mkdirs();

            System.out.println(">>> ĐƯỜNG DẪN THỰC TẾ LƯU FILE: " + fileReceive.getAbsolutePath());

            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(fileReceive));
            bos.write(fileInfo.getDataBytes());
            bos.flush();
            bos.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public FileInfo sendFile(String sourceFilePath) {
        try {
            File sourceFile = new File(sourceFilePath);
            if (!sourceFile.exists()) {
                System.out.println("File không tồn tại: " + sourceFilePath);
                return null;
            }

            FileInfo fileInfo = new FileInfo();
            fileInfo.setFilename(sourceFile.getName());

            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(sourceFile));
            byte[] fileBytes = new byte[(int) sourceFile.length()];
            bis.read(fileBytes, 0, fileBytes.length);
            bis.close();

            fileInfo.setDataBytes(fileBytes);

            os.writeObject(fileInfo);
            os.flush();
            System.out.println("Đã gửi file xong!");
            return fileInfo;

        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public String handleCheckCommand() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Danh sách file trên Server:\n");
        File folder = new File(config.getConfig("download.path"));
        if (folder.isDirectory()) {
            String[] files = folder.list();
            if (files != null && files.length > 0) {
                for (String name : files) {
                    stringBuilder.append("- ").append(name).append("\n");
                }
            } else {
                stringBuilder.append("(Thư mục trống)");
            }
        }
        return  stringBuilder.toString();
    }
}
