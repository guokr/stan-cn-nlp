package com.guokr.nlp;

import java.io.PrintStream;

import com.guokr.nlp.SegWrapper;
import com.guokr.nlp.NerWrapper;
import com.guokr.nlp.TagWrapper;

public class GkNlpCli {

    public static void main(String[] args) {

        if(args.length < 2) {
            usage();
        } else {
            String subcmd = args[0];
            String text = args[1];
            if(subcmd.equals("seg")) {
                System.out.println(SegWrapper.segment(text));
            } else if(subcmd.equals("ner")) {
                System.out.println(NerWrapper.recognize(text));
            } else if(subcmd.equals("tag")) {
                System.out.println(TagWrapper.tag(text));
            }
        }

    }

    private static void usage() {
        PrintStream out = System.out;
        out.println("ant [command] [text]");
        out.println("\tcommands:");
        out.println("\t\tseg: segment the text into words");
        out.println("\t\tner: recognize the named entity in the text");
        out.println("\t\ttag: tag the text into words with syntax information");
        out.println("");
    }

}
