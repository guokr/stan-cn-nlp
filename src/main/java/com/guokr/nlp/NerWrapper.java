package com.guokr.nlp;

import java.util.Properties;

import edu.stanford.nlp.ie.crf.CRFClassifier;

import com.guokr.util.Settings;

public class NerWrapper {

    public static Settings defaults = Settings.load("ner/defaults.using.prop");

    public static CRFClassifier reload(Properties settings, Properties defaults) {
        Settings props = new Settings(settings, defaults);
        String model = props.getProperty("model");
        CRFClassifier crf = null;
        try {
            crf = CRFClassifier.getClassifier(model, props);
        } catch (Exception e) {
            System.out.println(e);
        }
        return crf;
    }

    public static CRFClassifier classifier = reload(Settings.empty, defaults);

    public static String recognize(String text) {
        return classifier.classifyToString(SegWrapper.segment(text));
    }

}

