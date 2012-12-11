package com.guokr.nlp.commands;

import java.util.*;

import com.beust.jcommander.Parameters;
import com.beust.jcommander.Parameter;

@Parameters(commandDescription = "recognize input")
public class Ner {
    @Parameter(description = "content for recognization")
    public List<String> text = new ArrayList<String>();
}

