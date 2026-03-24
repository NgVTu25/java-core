package bt2;

import java.io.FileWriter;
import java.io.IOException;

public class OutputFileWriter {

    public void Writer(String fileContent) {
        try {
            FileWriter myWriter = new FileWriter("output.txt");
            myWriter.write(fileContent);
            myWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Successfully wrote to the file.");
        }
}
