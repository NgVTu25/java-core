package bt2;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Scanner;

public class InputFileReader {
    public static int[] readFile() throws IOException {
        Properties properties = new Properties();
        InputStream is = Main.class.getResourceAsStream("/config.properties");
        properties.load(is);
        String inputPath = properties.getProperty("input.path");
        Scanner sc = new Scanner(new File(inputPath));
        ArrayList<Integer> list = new ArrayList<>();

        while (sc.hasNextInt()) {
            list.add(sc.nextInt());
        }
        sc.close();
        int[] arr = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }
}
