package bt3;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

import static bt3.Server.broadcast;


public class ClientHandler implements Runnable {
    private Socket socket;
    private BlockingQueue<Socket> queue;
    private DataOutputStream os;
    private DataInputStream is;
    private ConfigReader config;
    private int id;

    public  ClientHandler(Socket socket, BlockingQueue<Socket> queue) {
        this.socket = socket;
        this.queue = queue;
        try {
            this.os = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        config = ConfigReader.getInstance();
    }

    public boolean handleFileAction(String action, String sourceFilePath) {
        String destinationDir = config.getConfig("download.path");
        return switch (action) {
            case "SAVE" -> {
                try {
                    config = ConfigReader.getInstance();
                    FileOutputStream fos = new FileOutputStream(config.getConfig("download.path"));
                    yield true;
                } catch (IOException e) {
                    e.printStackTrace();
                    yield false;
                }
            }

            case "SEND" -> {
                try {
                    sendFile(sourceFilePath, destinationDir);
                    yield true;
                } catch (Exception e) {
                    e.printStackTrace();
                    yield false;
                }
            }
            case "GET_INFO" -> {
                FileInfo f = getFileInfo(sourceFilePath, destinationDir);
                f.toString();
                yield (f != null);
            }
            default -> {
                System.err.println("Unknown action: " + action);
                yield false;
            }
        };
    }


    @Override
    public void run() {
        StringBuilder stringBuilder = new StringBuilder();
        String action;
        try {
            is = new DataInputStream(socket.getInputStream());
            while (true) {
                String str = is.readUTF();
                if (str.equalsIgnoreCase("stop")) {
                    break;
                }
                System.out.println("Type SENDFILE (Mode)");
                if (str.equalsIgnoreCase("SENDFILE")) {
                    System.out.println("SENDFILE MODE");
                    do {
                        action = is.readUTF();
                        String sourceFilePath = is.readUTF();
                        handleFileAction(action, sourceFilePath);
                    } while (action.equalsIgnoreCase("NONE"));
                }

                if (str.equalsIgnoreCase("CHECK")) {
                    File folder = new File(config.getConfig("download.path"));
                    if (folder.isDirectory()) {
                        String[] files = folder.list();
                        for (String name : files) {
                            stringBuilder.append(name).append("\n");
                        }
                    }
                    broadcast(stringBuilder.toString());
                }


                System.out.println("Client (" + socket.getRemoteSocketAddress() + "): " + str);

                broadcast("Client (" + socket.getPort() + ") says: " + str);
            }
        } catch (IOException e) {
            System.out.println("Client disconnected unexpectedly: " + socket.getRemoteSocketAddress());
        } finally {
            disconnect();
        }
    }

    public void sendMessage(String msg) {
        try {
            os.writeUTF(msg);
            os.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
//
//    public void getMessage() {
//        Thread mess = new Thread( () -> {
//            try {
//                DataInputStream is = new DataInputStream(socket.getInputStream());
//                while (true) {
//                    String str = is.readUTF();
//                    if (str.equals("stop")|| str == null) {
//                        break;
//                    }
//                    System.out.println("Client: " + socket.getLocalSocketAddress() + str);
//                }
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        });
//        mess.start();
//    }

    public void sendFile(String sourceFilePath, String destinationDir) {
        DataOutputStream outToServer;
        ObjectOutputStream oos;
        ObjectInputStream ois;

        try {
            // make greeting
            outToServer = new DataOutputStream(
                    socket.getOutputStream());
            outToServer.writeUTF("Hello from "
                    + socket.getLocalSocketAddress());

            // get file info
            FileInfo fileInfo = getFileInfo(sourceFilePath, destinationDir);

            // send file
            oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(fileInfo);

            // get confirmation
            ois = new ObjectInputStream(socket.getInputStream());
            fileInfo = (FileInfo) ois.readObject();
            if (fileInfo != null) {
                System.out.println(fileInfo.getStatus());
            }
        } catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    private FileInfo getFileInfo(String sourceFilePath, String destinationDir) {
        FileInfo fileInfo = null;
        BufferedInputStream bis;
        try {
            File sourceFile = new File(sourceFilePath);
            bis = new BufferedInputStream(new FileInputStream(sourceFile));
            fileInfo = new FileInfo();
            byte[] fileBytes = new byte[(int) sourceFile.length()];
            // get file info
            bis.read(fileBytes, 0, fileBytes.length);
            fileInfo.setFilename(sourceFile.getName());
            fileInfo.setDataBytes(fileBytes);
            fileInfo.setDestinationDirectory(destinationDir);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return fileInfo;
    }

    private void disconnect() {
        try {
            System.out.println("Closing connection for: " + socket.getRemoteSocketAddress());
            Server.clients.remove(this); // Remove from broadcast list
            queue.remove(socket);        // Free up space in the queue
            if (is != null) is.close();
            if (os != null) os.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
