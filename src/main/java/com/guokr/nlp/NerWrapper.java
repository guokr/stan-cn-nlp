package com.guokr.nlp;

import java.lang.reflect.Method;

import edu.stanford.nlp.ie.crf.CRFClassifier;

import com.guokr.util.Settings;

public class NerWrapper {

    private static Settings defaults = Settings.load("classpath:ner/defaults.using.prop");

    private CRFClassifier classifier;

    public NerWrapper(Settings settings) {
        Settings props = new Settings(settings, defaults);
        String model = props.getProperty("model");
        try {
            classifier = CRFClassifier.getClassifier(model, props);
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace(System.err);
        }
    }

    public String recognize(String text) {
        String segText = text;
        try {
            ClassLoader loader = getClass().getClassLoader().getParent();
            Object pkg = loader.loadClass("com.guokr.nlp.__PKG__").getEnumConstants()[0];
            Class pkgClazz = pkg.getClass();

            Object segObjct = pkgClazz.getDeclaredField("seg").get(pkg);
            Class segClazz  = (Class)pkgClazz.getDeclaredField("localSegWrapper").get(pkg);
            Method segMtd = segClazz.getDeclaredMethod("segment", String.class);

            segText = (String)segMtd.invoke(segObjct, text);
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace(System.err);
        }
        return classifier.classifyToString(segText).trim();
    }

}

