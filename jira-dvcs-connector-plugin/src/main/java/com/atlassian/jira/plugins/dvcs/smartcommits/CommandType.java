package com.atlassian.jira.plugins.dvcs.smartcommits;

public enum CommandType {
    TRANSITION("transition"),
    COMMENT("comment"),
    LOG_WORK("time");

    private String name;

    CommandType(String name) {
        this.name = name;
    }

    public static CommandType getCommandType(String commandString) {
        String commandNameTrimmed = commandString == null ? null : commandString.trim();
        if (commandNameTrimmed == null || commandNameTrimmed.equals("")) {
            return null;
        }

        if (commandNameTrimmed.equalsIgnoreCase(LOG_WORK.name)) {
            return LOG_WORK;
        } else if (commandNameTrimmed.equalsIgnoreCase(COMMENT.name)) {
            return COMMENT;
        } else {
            return TRANSITION;
        }
    }

    public String getName() {
        return name;
    }
}
