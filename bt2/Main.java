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
        int[] arr = fileReader.readFile();

            Thread[] threads = new Thread[number_of_Threads];
            int arraySize = (arr.length + number_of_Threads - 1) / number_of_Threads;

            long startTime = System.currentTimeMillis();

            for (int i = 0; i < number_of_Threads; i++) {
                int start = i * arraySize;

                int end = Math.min(start + arraySize - 1, arr.length - 1);

                threads[i] = new Thread(new ThreadSort(arr, start, end));
                threads[i].start();
            }

            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            if (number_of_Threads != 1) {
                ThreadSort threadSort = new ThreadSort(arr, 0, arr.length - 1);
            }
        long endTime = System.currentTimeMillis();
            long Time = endTime - startTime;

        System.out.println("Time taken: " + Time);


        OutputFileWriter outputFileWriter = new OutputFileWriter();
        outputFileWriter.Writer(Arrays.toString(arr));
    }
}