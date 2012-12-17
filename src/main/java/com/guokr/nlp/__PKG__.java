package com.guokr.nlp;

import java.security.ProtectionDomain;
import java.security.CodeSource;
import java.net.URL;

import org.xeustechnologies.jcl.JarClassLoader;
import org.xeustechnologies.jcl.JclObjectFactory;

import com.guokr.util.ClasspathProtocol;

public enum __PKG__ {

    INSTANCE;

    private ProtectionDomain domain = JarClassLoader.class.getProtectionDomain();
    private CodeSource source = domain.getCodeSource();
    private URL location = source.getLocation();
    private String path = location.getPath();
    private JarClassLoader loader = new JarClassLoader(new String[] { path });
    private JclObjectFactory factory = JclObjectFactory.getInstance();

    private ClasspathProtocol protocol = ClasspathProtocol.INSTANCE;
    private Class localSettings = loader.safeLoadClass("com.guokr.util.Settings");

    public Class localSegWrapper = loader.safeLoadClass("com.guokr.nlp.SegWrapper");
    public Class localNerWrapper = loader.safeLoadClass("com.guokr.nlp.NerWrapper");
    public Class localTagWrapper = loader.safeLoadClass("com.guokr.nlp.TagWrapper");

    public Object seg = factory.create(loader, "com.guokr.nlp.SegWrapper", new Object[]{null}, new Class[]{localSettings});
    public Object ner = factory.create(loader, "com.guokr.nlp.NerWrapper", new Object[]{null}, new Class[]{localSettings});
    public Object tag = factory.create(loader, "com.guokr.nlp.TagWrapper", new Object[]{null}, new Class[]{localSettings});

}

