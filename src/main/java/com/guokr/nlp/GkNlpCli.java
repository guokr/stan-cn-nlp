package com.guokr.nlp;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import com.guokr.nlp.SegWrapper;
import com.guokr.nlp.NerWrapper;
import com.guokr.nlp.TagWrapper;

import com.guokr.nlp.commands.Main;
import com.guokr.nlp.commands.Seg;
import com.guokr.nlp.commands.Ner;
import com.guokr.nlp.commands.Tag;

public class GkNlpCli {

    public JCommander jc;
    public Seg seg;
    public Ner ner;
    public Tag tag;

    public GkNlpCli() {
        Main main = new Main();
        this.jc = new JCommander(main);

        seg = new Seg();
        jc.addCommand("seg", seg);

        ner = new Ner();
        jc.addCommand("ner", ner);

        tag = new Tag();
        jc.addCommand("tag", tag);
    }

    public static final void main(String[] args) {
        GkNlpCli cli = new GkNlpCli();
        String subcmd = cli.jc.getParsedCommand();

        if(subcmd == null) {
            cli.jc.usage();
        } else if(subcmd.equals("seg")) {
            System.out.println(SegWrapper.segment(cli.seg.text));
        } else if(subcmd.equals("ner")) {
            System.out.println(NerWrapper.recognize(cli.ner.text));
        } else if(subcmd.equals("tag")) {
            System.out.println(TagWrapper.tag(cli.tag.text));
        }

    }

}
