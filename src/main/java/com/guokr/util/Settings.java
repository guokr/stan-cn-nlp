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
        try {
            Class.forName("com.guokr.util.ClasspathProtocol");
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace(System.err);
        }

        Properties props = new Properties();
        try {
            InputStream ins = new URL(uri).openStream();
            props.load(ins);
        } catch(Exception e) {
            e.printStackTrace(System.err);
        }
        return new Settings(props, empty);
    }

    public Settings(Properties currents, Properties defaults) {
        ClasspathProtocol.class.getName();

        this.defaults = defaults;
        Enumeration e = currents.propertyNames();
        while (e.hasMoreElements()) {
            String key = e.nextElement().toString();
            String value = currents.getProperty(key);

            value = translateClasspath(value);

            this.setProperty(key, value);
        }
    }

    public String translateClasspath(String value) {
        if (value.startsWith("classpath:")) {
            System.err.println("value:" + value);
            try {
                URL url = new URL(value);
                URLConnection conn = url.openConnection();
                value = conn.getURL().getPath();
            } catch (Exception e) {
                System.err.println(e);
                e.printStackTrace(System.err);
            }
            System.err.println("value:" + value);
        }
        return value;
    }

}
