package de.bischinger.jallablog;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static java.lang.Thread.currentThread;

/**
 * Created by bischofa on 10/01/16.
 */
public class PropertyHolder {

    private Properties configProperties;

    public PropertyHolder() throws IOException {
        configProperties = new Properties();
        InputStream input = currentThread().getContextClassLoader()
                .getResourceAsStream("de/bischinger/jallablog/config.properties");
        configProperties.load(input);
    }

    public String getProperty(String key) {
        return configProperties.getProperty(key);
    }
}
