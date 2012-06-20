package com.atlassian.jira.plugins.dvcs.smartcommits.model;

import javax.xml.bind.annotation.*;
import java.util.List;

@XmlRootElement()
@XmlAccessorType(XmlAccessType.FIELD)
public class CommitCommands {

    @XmlElementWrapper(name = "commands")
    @XmlElement(name = "command")
    List<CommitCommand> commands;

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
    }
}