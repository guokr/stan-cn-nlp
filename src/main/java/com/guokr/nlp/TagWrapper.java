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
        return tagger.tagString(__PKG__.INSTANCE.segment(text)).trim();
    }

}
