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
        return classifier.classifyToString(__PKG__.INSTANCE.segment(text).trim()).trim();
    }

}

