package bt1;

import java.util.concurrent.BlockingQueue;

public class Consumer implements Runnable {
    private final BlockingQueue<String> queue;
    public Consumer(BlockingQueue<String> queue) {
        this.queue = queue;
    }


    @Override
    public void run() {
        while (true) {
            try {
                String numberTakeOut = queue.take();
                System.out.println("Consumed resource - Queue size() = " + queue.size());
                System.out.printf("NumberTakeOut: %s\n", numberTakeOut);
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
