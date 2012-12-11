package com.guokr.nlp.commands;

import java.util.*;

import com.beust.jcommander.Parameters;
import com.beust.jcommander.Parameter;

@Parameters(commandDescription = "segment input")
public class Seg {
    @Parameter(description = "content for segmentation")
    public List<String> text = new ArrayList<String>();
}

