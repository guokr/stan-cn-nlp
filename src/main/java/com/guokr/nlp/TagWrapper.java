package com.guokr.nlp;

import java.io.IOException;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class TagWrapper {

    public static String model = ResUtil.get("chinese-distsim.tagger");

    private static MaxentTagger getTagger() {
        MaxentTagger mt = null;
        try {
            mt = new MaxentTagger(model);
        } catch (Exception e) {
        }
        return mt;
    }

    public static MaxentTagger tagger = getTagger();

    public static String tag(String text) {
        return tagger.tagString(SegWrapper.segment(text));
    }

}
