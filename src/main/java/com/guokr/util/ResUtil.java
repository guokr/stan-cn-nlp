package com.guokr.nlp;

import java.io.IOException;
import java.net.URL;

public class ResUtil {

    public static String get(String name) {
        URL url = ResUtil.class.getClassLoader().getResource(name);
        return url.getFile();
    }

}
