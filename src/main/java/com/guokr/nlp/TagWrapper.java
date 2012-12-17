package com.guokr.nlp;

import java.lang.reflect.Method;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;

import com.guokr.util.Settings;

public class TagWrapper {

    private static Settings defaults = Settings.load("classpath:tag/defaults.using.prop");

    private MaxentTagger tagger;

    public TagWrapper(Settings settings) {
        Settings props = new Settings(settings, defaults);
        String model = props.getProperty("model");
        MaxentTagger mt = null;
        try {
            tagger = new MaxentTagger(model);
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace(System.err);
        }
    }

    public String tag(String text) {
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
        return tagger.tagString(segText).trim();
    }

}
