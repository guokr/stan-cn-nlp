package com.guokr.nlp;

import java.util.Properties;

import edu.stanford.nlp.ie.crf.CRFClassifier;

public class SegWrapper {

    public static String dictionary = ResUtil.get("dict-chris6.ser.gz");
    public static String norm = ResUtil.get("norm.simp.utf8");
    public static String base = dictionary.substring(0, dictionary.lastIndexOf(java.io.File.separator));
    public static String model = ResUtil.get("ctb.gz");

    public static CRFClassifier reload() {
        Properties props = new Properties();
        props.setProperty("serDictionary", dictionary);
        props.setProperty("NormalizationTable", norm);
        props.setProperty("sighanCorporaDict", base);
        props.setProperty("sighanPostProcessing", "true");
        props.setProperty("inputEncoding", "UTF-8");
        props.setProperty("outputEncoding", "UTF-8");
        props.setProperty("normTableEncoding", "UTF-8");
        CRFClassifier crf = null;
        try {
            crf = CRFClassifier.getClassifier(model, props);
        } catch (Exception e) {
        }
        return crf;
    }

    public static CRFClassifier classifier = reload();

    public static String segment(String text) {
        return classifier.classifyToString(text);
    }
}
