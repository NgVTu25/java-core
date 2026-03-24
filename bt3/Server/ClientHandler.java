package bt3.Server;


import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientHandler implements Runnable {

    public static List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    @Override
    public void run() {

    }
}
