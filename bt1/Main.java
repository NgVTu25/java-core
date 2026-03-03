package bt1;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Main{
    public static void main(String[] args) throws Exception {
        int number_of_Threads = 10;
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(number_of_Threads);

        Producer producer = new Producer(queue, number_of_Threads);

        Consumer consumer = new Consumer(queue);
        Consumer consumer2 = new Consumer(queue);
        Consumer consumer3 = new Consumer(queue);

        new Thread(producer).start();
        new Thread(consumer).start();
        new Thread(consumer2).start();
        new Thread(consumer3).start();


    }
}