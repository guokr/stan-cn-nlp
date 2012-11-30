package com.guokr.nlp;

import java.io.IOException;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class TagWrapper {

    public static String model = ResUtil.get("chinese-distsim.tagger");

    public static MaxentTagger reload() {
        MaxentTagger mt = null;
        try {
            mt = new MaxentTagger(model);
        } catch (Exception e) {
        }
        return mt;
    }

    public static MaxentTagger tagger = reload();

    public static String tag(String text) {
        return tagger.tagString(SegWrapper.segment(text));
    }

}
