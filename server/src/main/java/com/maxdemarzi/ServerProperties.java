package com.maxdemarzi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class ServerProperties {

    public static Properties getProperties() throws IOException {

        Properties properties = new Properties();
        File file = new File("conf/server.properties");
        FileInputStream fis = new FileInputStream(file);

        if (fis != null) {
            properties.load(fis);
        } else {
            throw new FileNotFoundException("server.properties file not found");
        }

        return properties;
    }

}
