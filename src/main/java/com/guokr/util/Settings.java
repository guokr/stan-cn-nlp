package com.guokr.util;

import java.io.InputStream;
import java.io.FileInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Properties;

public class Settings extends Properties {

    public static Settings empty = new Settings(new Properties(), new Properties());

    public static Settings load(String uri) {
        Properties props = new Properties();
        try {
            System.err.println("before loading settings:" + uri);
            InputStream ins = new URL(uri).openStream();
            props.load(ins);
            System.err.println("after loading settings:" + uri);
        } catch(Exception e) {
            e.printStackTrace(System.err);
        }
        return new Settings(props, empty);
    }

    public Settings(Properties currents, Properties defaults) {
        this.defaults = defaults;
        if (currents != null) {
            Enumeration e = currents.propertyNames();
            while (e.hasMoreElements()) {
                String key = e.nextElement().toString();
                String value = currents.getProperty(key);

                value = translateClasspath(key, value);

                this.setProperty(key, value);
            }
        }
    }

    public String translateClasspath(String key, String value) {
        if (value.startsWith("classpath:") && value.length() > 10) {
            System.err.println("original:" + value);
            try {
                String path = null;
                URL url = new URL(value);
                URLConnection conn = url.openConnection();

                if (conn != null) {
                    path = conn.getURL().getPath();

                    if (key.equals("model") && path.contains("!") && path.endsWith(".gz")) {
                        path = path.substring(path.indexOf("!") + 2);
                        value = path;
                    }
                    if (!path.contains("!")) {
                        value = path;
                    }
                } else {
                    value = value.substring(10);
                }
            } catch (Exception e) {
                System.err.println(e);
                e.printStackTrace(System.err);
            }
            System.err.println("translated:" + value);
        }
        return value;
    }

}
