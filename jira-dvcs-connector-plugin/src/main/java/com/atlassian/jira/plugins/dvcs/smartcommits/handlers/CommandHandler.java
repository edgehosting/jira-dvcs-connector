package com.atlassian.jira.plugins.dvcs.smartcommits.handlers;

import java.util.List;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.plugins.dvcs.smartcommits.CommandType;
import com.atlassian.jira.plugins.dvcs.smartcommits.model.CommitHookErrors;
import com.atlassian.jira.plugins.dvcs.smartcommits.model.Either;

public interface CommandHandler<T> {
    
	CommandType getCommandType();
    
    Either<CommitHookErrors, T> handle(User user, MutableIssue issue, String commandName, List<String> args);
}
