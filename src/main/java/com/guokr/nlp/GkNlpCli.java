package com.guokr.nlp;

import java.lang.reflect.Method;
import java.io.PrintStream;

public class GkNlpCli {

    public static void main(String[] args) {
        __PKG__ pkg = __PKG__.INSTANCE;
        if(args.length < 2) {
            usage();
        } else {
            String subcmd = args[0];
            String text = args[1];
            if(subcmd.equals("seg")) {
                try {
                    String segText = pkg.segment(text);
                    System.out.println(segText);
                } catch (Exception e) {
                    System.out.println(e);
                    e.printStackTrace(System.err);
                }
            } else if(subcmd.equals("ner")) {
                try {
                    String nerText = pkg.recognize(text);
                    System.out.println(nerText);
                } catch (Exception e) {
                    System.out.println(e);
                    e.printStackTrace(System.err);
                }
            } else if(subcmd.equals("tag")) {
                try {
                    String tagText = pkg.tag(text);
                    System.out.println(tagText);
                } catch (Exception e) {
                    System.out.println(e);
                    e.printStackTrace(System.err);
                }
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
