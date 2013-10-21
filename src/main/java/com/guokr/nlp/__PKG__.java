package com.guokr.nlp;

public enum __PKG__ {

    INSTANCE;

    public String segment(String text) {
        return com.guokr.nlp.seg.__SEG__.INSTANCE.segment(text);
    }

    public String recognize(String text) {
        return com.guokr.nlp.ner.__NER__.INSTANCE.recognize(text);
    }

    public String tag(String text) {
        return com.guokr.nlp.tag.__TAG__.INSTANCE.tag(text);
    }

}

