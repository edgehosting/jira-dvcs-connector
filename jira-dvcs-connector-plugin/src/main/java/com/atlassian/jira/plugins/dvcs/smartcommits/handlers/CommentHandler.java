package com.atlassian.jira.plugins.dvcs.smartcommits.handlers;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.issue.comment.CommentService;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.plugins.dvcs.smartcommits.CommandType;
import com.atlassian.jira.plugins.dvcs.smartcommits.model.CommitHookErrors;
import com.atlassian.jira.plugins.dvcs.smartcommits.model.Either;

import java.util.List;

public class CommentHandler implements CommandHandler<Comment> {

    private static CommandType CMD_TYPE = CommandType.COMMENT;

    private CommentService commentService;

    public CommentHandler(CommentService commentService) {
        this.commentService = commentService;
    }


    public CommandType getCommandType() {
        return CMD_TYPE;
    }

    public Either<CommitHookErrors, Comment> handle(User user, MutableIssue issue, String commandName, List<String> args) {
        JiraServiceContextImpl jiraServiceContext = new JiraServiceContextImpl(user);
        Comment comment = commentService.create(user, issue, args.isEmpty() ? null : args.get(0), true, jiraServiceContext.getErrorCollection());

        if (jiraServiceContext.getErrorCollection().hasAnyErrors()) {
            return Either.error(CommitHookErrors.fromErrorCollection(
                    CMD_TYPE.getName(), issue.getKey(), jiraServiceContext.getErrorCollection()));
        } else {
            return Either.value(comment);
        }
    }
}
