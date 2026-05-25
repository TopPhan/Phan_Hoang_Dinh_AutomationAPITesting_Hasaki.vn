package com.helper;

import java.io.*;
import java.util.LinkedList;
import java.util.Properties;

public class PropertiesHelper {

    private static final String DEFAULT_PATH = "src/test/resources/configs/configs.properties";

    private static Properties properties;
    private static String     activeFilePath;
    private static FileInputStream fileInputStream;
    private static FileOutputStream fileOutputStream;

    public static Properties loadAllFiles() {
        LinkedList<String> filePaths = new LinkedList<>();
        filePaths.add("src/test/resources/configs/configs.properties");

        properties = new Properties();
        try {
            for (String path : filePaths) {
                Properties temp = new Properties();
                String absPath  = SystemHelper.getCurrentDir() + path;
                try (FileInputStream in = new FileInputStream(absPath)) {
                    temp.load(in);
                }
                properties.putAll(temp);
            }
        } catch (IOException e) {
            properties = new Properties();
        }
        return properties;
    }

    public static void setFile(String relPath) {
        properties      = new Properties();
        activeFilePath  = SystemHelper.getCurrentDir() + relPath;
        try (FileInputStream in = new FileInputStream(activeFilePath)) {
            properties.load(in);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setDefaultFile() {
        setFile(DEFAULT_PATH);
    }

    public static String getValue(String key) {
        try {
            if (fileInputStream == null) {
                properties      = new Properties();
                activeFilePath  = SystemHelper.getCurrentDir() + DEFAULT_PATH;
                fileInputStream = new FileInputStream(activeFilePath);
                properties.load(fileInputStream);
                fileInputStream.close();
                fileInputStream = null;
            }
            return properties.getProperty(key);
        } catch (Exception e) {
            System.out.println("PropertiesHelper.getValue error: " + e.getMessage());
            return null;
        }
    }

    public static void setValue(String key, String value) {
        try {
            if (fileInputStream == null) {
                properties      = new Properties();
                activeFilePath  = SystemHelper.getCurrentDir() + DEFAULT_PATH;
                fileInputStream = new FileInputStream(activeFilePath);
                properties.load(fileInputStream);
                fileInputStream.close();
                fileInputStream = null;
            }
            properties.setProperty(key, value);
            try (FileOutputStream out = new FileOutputStream(activeFilePath)) {
                properties.store(out, null);
            }
        } catch (Exception e) {
            System.out.println("PropertiesHelper.setValue error: " + e.getMessage());
        }
    }
}
