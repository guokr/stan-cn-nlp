package com.guokr.nlp.commands;

import com.beust.jcommander.Parameters;
import com.beust.jcommander.Parameter;

@Parameters(commandDescription = "tag input")
public class Tag {
    @Parameter(description = "content for tagging")
    public String text;
}

