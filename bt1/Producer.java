package bt1;

import java.util.Random;
import java.util.concurrent.BlockingQueue;

public class Producer implements Runnable {

    private final BlockingQueue<String> queue;
    public Producer(BlockingQueue<String> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        while (true) {
            try {
                String number = produce();
                queue.put(number);
                System.out.println("Produced resource - Queue size() = "  + queue.size());
                System.out.printf("bt1.Producer Message: %s\n", number);
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private String produce() {
        Random random = new Random();
        return String.valueOf(random.nextInt(100));
    }

}
