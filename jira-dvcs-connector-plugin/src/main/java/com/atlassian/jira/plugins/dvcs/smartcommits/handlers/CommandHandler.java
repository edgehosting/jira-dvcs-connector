package com.atlassian.jira.plugins.dvcs.smartcommits.handlers;

import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.plugins.dvcs.smartcommits.CommandType;
import com.atlassian.jira.plugins.dvcs.smartcommits.model.CommitHookHandlerError;
import com.atlassian.jira.plugins.dvcs.smartcommits.model.Either;
import com.atlassian.jira.user.ApplicationUser;

import java.util.Date;
import java.util.List;

public interface CommandHandler<T>
{
    CommandType getCommandType();

    Either<CommitHookHandlerError, T> handle(ApplicationUser user, MutableIssue issue, String commandName, List<String> args, Date commitDate);
}
