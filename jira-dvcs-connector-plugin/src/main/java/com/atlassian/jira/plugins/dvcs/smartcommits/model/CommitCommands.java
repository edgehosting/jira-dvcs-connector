package com.atlassian.jira.plugins.dvcs.smartcommits.model;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement()
@XmlAccessorType(XmlAccessType.FIELD)
public class CommitCommands {

    @XmlElementWrapper(name = "commands")
    @XmlElement(name = "command")
    List<CommitCommand> commands;
    
    String author;

    public String getAuthor()
	{
		return author;
	}

	public void setAuthor(String author)
	{
		this.author = author;
	}

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

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class CommitCommand {

        String issueKey;
        String commandName;

        @XmlElementWrapper(name = "arguments")
        @XmlElement(name = "argument")
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
        	return issueKey + " " + commandName + " args [ " + arguments + "]";
        }
    }
}