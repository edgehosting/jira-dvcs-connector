package com.atlassian.jira.plugins.dvcs.smartcommits.model;

import java.util.List;

public class CommitCommands {

    List<CommitCommand> commands;
    
    private String authorEmail;
    

	public void setCommands(List<CommitCommand> commands)
	{
		this.commands = commands;
	}

	public CommitCommands(List<CommitCommand> commands) {
        this.commands = commands;
    }

    public CommitCommands() {

    }

    public List<CommitCommand> getCommands() {
        return commands;
    }

    public static class CommitCommand {

        String issueKey;
        String commandName;

        List<String> arguments;

        public CommitCommand(String issueKey, String commandName, List<String> arguments) {
            this.issueKey = issueKey;
            this.commandName = commandName;
            this.arguments = arguments;
        }

        public CommitCommand() {

        }

        public String getIssueKey() {
            return issueKey;
        }

        public String getCommandName() {
            return commandName;
        }

        public List<String> getArguments() {
            return arguments;
        }
        
        @Override
        public String toString()
        {
        	return issueKey + " <<" + commandName + ">> args [ " + arguments + "]";
        }
    }

	public String getAuthorEmail()
	{
		return authorEmail;
	}

	public void setAuthorEmail(String authorEmail)
	{
		this.authorEmail = authorEmail;
	}
}