package bt3;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;


public class FileService {
    private ConfigReader config;
    private Socket client;


    public FileService(ServerSocket serverSocket) {
    }

    public boolean fileSave(String fileName) throws IOException {
        config = ConfigReader.getInstance();
        try {
            FileOutputStream fos = new FileOutputStream(config.getConfig("download.path"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return true;
    }


    public boolean createFiles(FileInfo fileInfo) {
        BufferedOutputStream bos = null;

        try {
            if (fileInfo != null) {
                File fileReceive = new File(fileInfo.getDestinationDirectory()
                        + fileInfo.getFilename());
                bos = new BufferedOutputStream(
                        new FileOutputStream(fileReceive));
                // write file content
                bos.write(fileInfo.getDataBytes());
                bos.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private File getFileInfo(String sourceFilePath, String fileName) {
        File fileInfo = null;
        BufferedInputStream bis = null;
        try {
            File sourceFile = new File(sourceFilePath);
            bis = new BufferedInputStream(new FileInputStream(sourceFile));
            byte[] fileBytes = new byte[(int) sourceFile.length()];
            bis.read(fileBytes, 0, fileBytes.length);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return fileInfo;
    }

    public void sendFile(String sourceFilePath, String destinationDir) {
        DataOutputStream outToServer = null;
        ObjectOutputStream oos = null;
        ObjectInputStream ois = null;

        try {
            outToServer = new DataOutputStream(
                    client.getOutputStream());
            outToServer.writeUTF("Hello from "
                    + client.getLocalSocketAddress());

            // get file info
            File fileInfo = getFileInfo(sourceFilePath, destinationDir);

            // sendMessage file
            oos = new ObjectOutputStream(client.getOutputStream());
            oos.writeObject(fileInfo);

            // get confirmation
            ois = new ObjectInputStream(client.getInputStream());
            fileInfo = (File) ois.readObject();

        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
