package bt3;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigReader {
    private static final ConfigReader instance = new ConfigReader();
    private Properties prop;
    private ConfigReader() {
        prop = new Properties();
        try {
            InputStream input = ConfigReader.class
                    .getClassLoader()
                    .getResourceAsStream("config.properties");
            prop.load(input);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static synchronized ConfigReader getInstance() {
        return instance;
    }

    public String getConfig(String key) {
        return prop.getProperty(key);
    }
}
