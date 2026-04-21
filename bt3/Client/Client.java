package bt3.Client;

import bt3.ConfigReader;
import bt3.MessageEnvelope;
import bt3.Server.FileService;
import bt3.common.EventType;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static bt3.common.EventType.*;

public class Client implements Runnable {

    private final BlockingQueue<Socket> queue;
    private final ConfigReader config = ConfigReader.getInstance();
    private ObjectInputStream is;
    private Socket socket;
    private BufferedReader br;
    private ObjectOutputStream os;


    public Client() {
        this.queue = new ArrayBlockingQueue<>(Integer.parseInt(config.getConfig("num.threads")));
    }

    public static void main(String[] args) {
        new Client().run();
    }

    @Override
    public void run() {
        String ip = "localhost";
        try {
            socket = new Socket(ip, Integer.parseInt(config.getConfig("port")));

            os = new ObjectOutputStream(socket.getOutputStream());

            is = new ObjectInputStream(socket.getInputStream());

            br = new BufferedReader(new InputStreamReader(System.in));
            os.flush();

            Thread receiveThread =
                    new Thread(() -> {
                        try {
                            while (true) {
                                Object obj = is.readObject();
                                if(obj instanceof MessageEnvelope(EventType type, Object payload)){
                                    switch(type){

                                        case CHAT -> System.out.println("\n[Tin nhắn]: " + payload);

                                        case PRIVATE_CHAT -> System.out.println("\n[Private]: " + payload);

                                        case CLIENT_LIST -> System.out.println("\n[Danh sách ID]: " + payload);

                                        case ACCEPT -> System.out.println("\n[ACCEPT]: " + payload);

                                        case REJECT -> System.out.println("\n[REJECT]: " + payload);

                                        case SERVER_FULL -> {
                                            System.out.println("\nServer đã đầy.");
                                            closeConnection();
                                            System.exit(0);
                                        }

                                        case STOP -> {System.out.println("\nServer đóng kết nối.");
                                            closeConnection();
                                            System.exit(0);
                                        }

                                        default -> System.out.println("\nUnknown event: " + type);
                                    }
                                } else{
                                    queue.put((Socket) obj);
                                }
                            }
                        } catch(Exception e){
                            System.out.println("\n[Hệ thống] Đã ngắt luồng nhận.");
                        }
                    });

            receiveThread.start();



            while(true){

                System.out.println("\n=== HỆ THỐNG QUẢN LÝ FILE & CHAT ===");

                System.out.println("1. CHECK");

                System.out.println("2. DOWNLOAD");

                System.out.println("3. UPLOAD");

                System.out.println("4. CHAT");

                System.out.println("5. PRIVATE CHAT");

                System.out.println("6. LIST CLIENT");

                System.out.println("7. STOP");

                System.out.print("Chọn (1-7): ");


                FileService fileService = new FileService(queue, os);


                String file;

                String choice = br.readLine().trim();

                switch(choice){

                    case "1":
                        fileService.requestCheck();
                        break;

                    case "2":
                        System.out.print("Nhập tên file: ");
                        file = br.readLine().trim();

                        if(file.isEmpty()){
                            System.out.println("Tên file trống.");
                            break;
                        }

                        fileService.requestDownload(file);
                        break;

                    case "3":
                        System.out.print("Nhập path file: ");

                        file = br.readLine().trim();

                        if(file.isEmpty()){
                            System.out.println("Path trống.");
                            break;
                        }

                        fileService.requestUpload(file);
                        break;

                    case "4":
                        fileService.chatMode();
                        break;

                    case "5":
                        System.out.println("Enter Client ID:");

                        int receiverId = Integer.parseInt(br.readLine());

                        while(true){
                            String message = br.readLine();
                            if(message.equalsIgnoreCase("stop")){
                                break;
                            }

                            fileService.clientToClient(receiverId, message);
                        }
                        break;

                    case "6":
                        fileService.requestClientIds();
                        break;

                    case "7":
                        os.writeObject(new MessageEnvelope(STOP, null));
                        os.flush();
                        closeConnection();
                        System.exit(0);

                        break;

                    default: System.out.println("Không hợp lệ.");
                }

            }

        } catch(Exception e){
            System.out.println("Lỗi kết nối: " + e.getMessage());
        }
    }

    private void closeConnection(){
        try{
            if(br!=null)
            br.close();
        }
        catch(IOException ignored){}

        try{
            if(is!=null)
                is.close();
        } catch(IOException ignored){}

        try{
            if(os!=null)
                os.close();
        } catch(IOException ignored){}

        try{
            if(socket!=null && !socket.isClosed()){
                socket.close();
            }
        } catch(IOException ignored){}

    }

}