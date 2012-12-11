package com.guokr.nlp.commands;

import java.util.*;

import com.beust.jcommander.Parameters;
import com.beust.jcommander.Parameter;

@Parameters(commandDescription = "tag input")
public class Tag {
    @Parameter(description = "content for tagging")
    public List<String> text = new ArrayList<String>();
}

