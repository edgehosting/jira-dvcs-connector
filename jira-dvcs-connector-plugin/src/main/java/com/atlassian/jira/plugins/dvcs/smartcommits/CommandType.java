package com.atlassian.jira.plugins.dvcs.smartcommits;

import org.apache.commons.lang.StringUtils;

public enum CommandType 
{
    TRANSITION("transition"), 
    COMMENT("comment"), 
    LOG_WORK("time");

    private String name;

    CommandType(String name)
    {
        this.name = name;
    }

    public static CommandType getCommandType(String commandString)
    {
        
        String commandNameTrimmed = StringUtils.trim(commandString);
        if (StringUtils.isBlank(commandNameTrimmed))
        {
            return null;
        }

        if (LOG_WORK.name.equalsIgnoreCase(commandNameTrimmed))
        {
            return LOG_WORK;
        } else if (COMMENT.name.equalsIgnoreCase(commandNameTrimmed))
        {
            return COMMENT;
        } else
        {
            return TRANSITION;
        }
    }

    public String getName()
    {
        return name;
    }
}
