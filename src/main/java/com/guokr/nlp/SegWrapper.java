package com.guokr.nlp;

import edu.stanford.nlp.ie.crf.CRFClassifier;

import com.guokr.util.Settings;

public class SegWrapper {

    public static Settings defaults = Settings.load("classpath:seg/defaults.using.prop");

    private CRFClassifier classifier;

    public SegWrapper(Settings settings) {
        Settings props = new Settings(settings, defaults);
        String model = props.getProperty("model");
        try {
            classifier = CRFClassifier.getClassifier(model, props);
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace(System.err);
        }
    }

    public String segment(String text) {
        System.err.println("---------------------------");
        return classifier.classifyToString(text).trim();
    }

}
