package com.guokr.nlp.commands;

import com.beust.jcommander.Parameters;
import com.beust.jcommander.Parameter;

@Parameters(commandDescription = "recognize input")
public class Ner {
    @Parameter(description = "content for recognization")
    public String text;
}

