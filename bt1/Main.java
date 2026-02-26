package bt1;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Main{
    public static void main(String[] args) throws Exception {
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(3);

        Producer producer = new Producer(queue);
        Consumer consumer = new Consumer(queue);
        Consumer consumer2 = new Consumer(queue);
        Consumer consumer3 = new Consumer(queue);

        new Thread(producer).start();
        new Thread(consumer).start();
        new Thread(consumer2).start();
        new Thread(consumer3).start();


    }
}