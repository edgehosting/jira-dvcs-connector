package com.atlassian.jira.plugins.dvcs.smartcommits.handlers;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.plugins.dvcs.smartcommits.CommandType;
import com.atlassian.jira.plugins.dvcs.smartcommits.model.CommitHookErrors;
import com.atlassian.jira.plugins.dvcs.smartcommits.model.Either;

import java.util.List;

public interface CommandHandler<T> {
    public abstract CommandType getCommandType();
    public abstract Either<CommitHookErrors, T> handle(User user, MutableIssue issue, String commandName, List<String> args);
}
