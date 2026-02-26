package bt2;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;


public class Main {

    public static void main(String[] args) throws IOException {

        Properties properties = new Properties();
        InputStream is = Main.class.getResourceAsStream("/config.properties");
        properties.load(is);

        int number_of_Threads = Integer.parseInt(properties.getProperty("num.threads"));

        InputFileReader fileReader = new InputFileReader();
        int[] originalArr = fileReader.readFile();

            int[] arr = Arrays.copyOf(originalArr, originalArr.length);

            Thread[] threads = new Thread[number_of_Threads];
            int chunkSize = (arr.length + number_of_Threads - 1) / number_of_Threads;

            long startTime = System.nanoTime();

            for (int i = 0; i < number_of_Threads; i++) {
                int start = i * chunkSize;

                if (start >= arr.length)
                    continue;

                int end = Math.min(start + chunkSize - 1, arr.length - 1);

                threads[i] = new Thread(new MergeSort(arr, start, end));
                threads[i].start();
            }

            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

        try {
            finalMerge(arr, chunkSize);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        long endTime = System.nanoTime();
            long Time = endTime - startTime;



        OutputFileWriter outputFileWriter = new OutputFileWriter();
        outputFileWriter.Writer( "Benchmark: " + Time + " ns");
    }


    public static void finalMerge(int[] arr, int chunkSize) throws InterruptedException {

        int size = chunkSize;
        while (size < arr.length) {
            int numMerges =
                    (arr.length + 2 * size - 1) / (2 * size);
            Thread[] threads = new Thread[numMerges];
            for (int i = 0; i < numMerges; i++) {
                int left = i * 2 * size;
                int mid = left + size - 1;
                if (mid >= arr.length)
                    continue;
                int right = Math.min(left + 2 * size - 1, arr.length - 1);
                threads[i] = new Thread(new Merge(arr, left, mid, right));
                threads[i].start();
            }
            for (Thread t : threads)
                if (t != null)
                    t.join();
            size *= 2;
        }
    }
}