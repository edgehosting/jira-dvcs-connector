package com.atlassian.jira.plugins.dvcs.smartcommits.handlers;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.issue.worklog.WorklogInputParametersImpl;
import com.atlassian.jira.bc.issue.worklog.WorklogResult;
import com.atlassian.jira.bc.issue.worklog.WorklogService;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.worklog.Worklog;
import com.atlassian.jira.plugins.dvcs.smartcommits.CommandType;
import com.atlassian.jira.plugins.dvcs.smartcommits.model.CommitHookErrors;
import com.atlassian.jira.plugins.dvcs.smartcommits.model.Either;

import java.util.Date;
import java.util.List;

public class WorkLogHandler implements CommandHandler<Worklog> {

    private static CommandType CMD_TYPE = CommandType.LOG_WORK;

    private WorklogService worklogService;

    public WorkLogHandler(WorklogService worklogService) {
        this.worklogService = worklogService;
    }


    public CommandType getCommandType() {
        return CMD_TYPE;
    }

    public Either<CommitHookErrors, Worklog> handle(User user, MutableIssue issue, String commandName, List<String> args) {
        JiraServiceContextImpl jiraServiceContext = new JiraServiceContextImpl(user);
        WorklogResult result = worklogService.validateCreate(
                jiraServiceContext,
                WorklogInputParametersImpl.builder()
                        .issue(issue)
                        .timeSpent(args.isEmpty() ? null : args.get(0))
                        .startDate(new Date())
                        .build());
        if (!jiraServiceContext.getErrorCollection().hasAnyErrors()) {
            return Either.value(worklogService.createAndAutoAdjustRemainingEstimate(
                    jiraServiceContext, result, true));
        } else {
            return Either.error(CommitHookErrors.fromErrorCollection(
                    CMD_TYPE.getName(), issue.getKey(), jiraServiceContext.getErrorCollection()));
        }
    }
}
