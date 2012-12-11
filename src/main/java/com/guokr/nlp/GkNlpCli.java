package com.guokr.nlp;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import com.guokr.nlp.SegWrapper;
import com.guokr.nlp.NerWrapper;
import com.guokr.nlp.TagWrapper;

import com.guokr.nlp.commands.Seg;
import com.guokr.nlp.commands.Ner;
import com.guokr.nlp.commands.Tag;

public class GkNlpCli {

    public static void main(String[] args) {
        JCommander jc = new JCommander();
        Seg seg = new Seg();
        jc.addCommand("seg", seg);

        Ner ner = new Ner();
        jc.addCommand("ner", ner);

        Tag tag = new Tag();
        jc.addCommand("tag", tag);

        jc.parse(args);
        String subcmd = jc.getParsedCommand();
        if(subcmd == null) {
            jc.usage();
        } else if(subcmd.equals("seg")) {
            System.out.println(SegWrapper.segment(seg.text.get(0)));
        } else if(subcmd.equals("ner")) {
            System.out.println(NerWrapper.recognize(ner.text.get(0)));
        } else if(subcmd.equals("tag")) {
            System.out.println(TagWrapper.tag(tag.text.get(0)));
        }

    }

}
