package com.guokr.nlp;

import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.security.CodeSource;
import java.net.URL;

import org.xeustechnologies.jcl.AbstractClassLoader;
import org.xeustechnologies.jcl.JarClassLoader;
import org.xeustechnologies.jcl.JclObjectFactory;

import com.guokr.util.DowngradeClassLoader;

public enum __PKG__ {

    INSTANCE;

    private ProtectionDomain domain = JarClassLoader.class.getProtectionDomain();
    private CodeSource source = domain.getCodeSource();
    private URL location = source.getLocation();
    private String path = (location != null ? location.getPath() : null);
    private AbstractClassLoader loader = (path != null
                                             ? new JarClassLoader(new String[] { path })
                                             : new DowngradeClassLoader(getClass().getClassLoader()));
    private JclObjectFactory factory = JclObjectFactory.getInstance();

    //private ClasspathProtocol protocol = ClasspathProtocol.INSTANCE;
    private Object protocol = loader.safeLoadClass("com.guokr.util.ClasspathProtocol").getEnumConstants()[0];
    private Class localSettings = loader.safeLoadClass("com.guokr.util.Settings");
    private Class localIOUtils = loader.safeLoadClass("edu.stanford.nlp.io.IOUtils");

    private Class localSegWrapper = loader.safeLoadClass("com.guokr.nlp.SegWrapper");
    private Class localNerWrapper = loader.safeLoadClass("com.guokr.nlp.NerWrapper");
    private Class localTagWrapper = loader.safeLoadClass("com.guokr.nlp.TagWrapper");

    private Object seg = (path != null
                             ? factory.create((JarClassLoader)loader, "com.guokr.nlp.SegWrapper", new Object[]{null}, new Class[]{localSettings})
                             : new SegWrapper(null));
    private Object ner = (path != null
                             ? factory.create((JarClassLoader)loader, "com.guokr.nlp.NerWrapper", new Object[]{null}, new Class[]{localSettings})
                             : new NerWrapper(null));
    private Object tagger = (path != null
                                ? factory.create((JarClassLoader)loader, "com.guokr.nlp.TagWrapper", new Object[]{null}, new Class[]{localSettings})
                                : new TagWrapper(null));

    public String segment(String text) {
        String result = null;
        try {
            Class[] argTypes = new Class[] { String.class };
            Method mtd = this.localSegWrapper.getDeclaredMethod("segment", argTypes);
            String[] args = new String[]{ text };
            result = mtd.invoke(this.seg, args).toString();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        return result;
    }

    public String recognize(String text) {
        String result = null;
        try {
            Class[] argTypes = new Class[] { String.class };
            Method mtd = this.localNerWrapper.getDeclaredMethod("recognize", argTypes);
            String[] args = new String[]{ text };
            result = mtd.invoke(this.ner, args).toString();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        return result;
    }

    public String tag(String text) {
        String result = null;
        try {
            Class[] argTypes = new Class[] { String.class };
            Method mtd = this.localTagWrapper.getDeclaredMethod("tag", argTypes);
            String[] args = new String[]{ text };
            result = mtd.invoke(this.tagger, args).toString();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        return result;
    }

}

