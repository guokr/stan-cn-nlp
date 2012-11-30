package com.guokr.nlp;

import java.util.Properties;

import edu.stanford.nlp.ie.crf.CRFClassifier;

public class NerWrapper {

    public static String model = ResUtil.get("chinese.misc.distsim.crf.ser.gz");

    private static CRFClassifier getClassifier() {
        Properties props = new Properties();
        props.setProperty("inputEncoding", "UTF-8");
        props.setProperty("outputEncoding", "UTF-8");
        CRFClassifier crf = null;
        try {
            crf = CRFClassifier.getClassifier(model, props);
        } catch (Exception e) {
        }
        return crf;
    }

    public static CRFClassifier classifier = getClassifier();

    public static String recognize(String text) {
        return classifier.classifyToString(SegWrapper.segment(text));
    }

}

