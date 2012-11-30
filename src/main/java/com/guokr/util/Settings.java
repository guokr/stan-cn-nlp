package com.guokr.util;

import java.util.Enumeration;
import java.util.Properties;

public class Settings extends Properties {

    public static Settings empty = new Settings(new Properties(), new Properties());

    public static Settings load(String name) {
        Properties props = new Properties();
        try {
            props.load(Settings.class.getResourceAsStream(name));
        } catch(Exception e) {
        }
        return new Settings(props, empty);
    }

    public Settings(Properties currents, Properties defaults) {
        this.defaults = defaults;
        Enumeration e = currents.propertyNames();
        while (e.hasMoreElements()) {
            String key = e.nextElement().toString();
            String value = currents.getProperty(key);
            this.setProperty(key, value);
        }
    }

}
