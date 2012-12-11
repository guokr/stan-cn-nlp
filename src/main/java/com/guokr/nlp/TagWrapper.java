package com.guokr.nlp;

import java.util.Properties;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;

import com.guokr.util.Settings;
import com.guokr.util.ClasspathProtocol;

public class TagWrapper {

    public static Settings defaults = Settings.load("src/main/resources/tag/defaults.using.prop");

    public static MaxentTagger reload(Properties settings, Properties defaults) {
        ClasspathProtocol.class.getName();

        Settings props = new Settings(settings, defaults);
        String model = props.getProperty("model");
        MaxentTagger mt = null;
        try {
            mt = new MaxentTagger(model);
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace(System.out);
        }
        return mt;
    }

    public static MaxentTagger tagger = reload(Settings.empty, defaults);

    public static String tag(String text) {
        return tagger.tagString(SegWrapper.segment(text)).trim();
    }

}
