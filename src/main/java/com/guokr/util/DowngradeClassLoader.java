package com.guokr.util;

import org.xeustechnologies.jcl.AbstractClassLoader;

public class DowngradeClassLoader extends AbstractClassLoader {

    public DowngradeClassLoader(ClassLoader delegate) {
        super(delegate);
    }

}
