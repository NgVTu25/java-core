package bt2;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;


public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {

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
            if (start >= arr.length)
                break;
            threads[i] = new Thread(new ThreadSort(arr, start, end));
            threads[i].start();
        }

        for (Thread t : threads) {
            if (t != null)
                t.join();
        }


        int currentSize = arraySize;

        while (currentSize < arr.length) {
            for (int leftStart = 0; leftStart < arr.length - 1; leftStart += 2 * currentSize) {
                int mid = Math.min(leftStart + currentSize - 1, arr.length - 1);
                int rightEnd = Math.min(leftStart + 2 * currentSize - 1, arr.length - 1);
                if (mid < rightEnd) {
                    mergeSort(arr, leftStart, mid, rightEnd);
                }
            }
            currentSize *= 2;
        }

        long endTime = System.currentTimeMillis();
            long Time = endTime - startTime;

        System.out.println("Time taken: " + Time);


        OutputFileWriter outputFileWriter = new OutputFileWriter();
        outputFileWriter.Writer(Arrays.toString(arr));
    }

    public static void mergeSort(int[] arr, int start, int mid, int end) {

        int[] temp = new int[end - start + 1];

        int i = start;
        int j = mid + 1;
        int k = 0;

        while (i <= mid && j <= end) {
            if (arr[i] <= arr[j]) {
                temp[k++] = arr[i++];
            } else {
                temp[k++] = arr[j++];
            }
        }

        while (i <= mid)
            temp[k++] = arr[i++];

        while (j <= end)
            temp[k++] = arr[j++];

        for (int x = 0; x < temp.length; x++)
            arr[start + x] = temp[x];
    }

}