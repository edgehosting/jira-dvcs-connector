package com.atlassian.jira.plugins.dvcs.smartcommits.handlers;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.issue.comment.CommentService;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.plugins.dvcs.smartcommits.CommandType;
import com.atlassian.jira.plugins.dvcs.smartcommits.model.CommitHookHandlerError;
import com.atlassian.jira.plugins.dvcs.smartcommits.model.Either;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@ExportAsService (CommandHandler.class)
@Component ("smartcommitsCommentHandler")
public class CommentHandler implements CommandHandler<Comment>
{

    private static CommandType CMD_TYPE = CommandType.COMMENT;

    private final CommentService commentService;

    @Autowired
    public CommentHandler(@ComponentImport CommentService commentService)
    {
        this.commentService = commentService;
    }

    @Override
	public CommandType getCommandType() {
        return CMD_TYPE;
    }

    @Override
	public Either<CommitHookHandlerError, Comment> handle(User user, MutableIssue issue, String commandName, List<String> args, Date commitDate) {

    	JiraServiceContextImpl jiraServiceContext = new JiraServiceContextImpl(user);
       
        Comment comment = commentService.create(user,
                                                issue,
                                                args.isEmpty() ? null : args.get(0),
                                                null, null, commitDate,
                                                true,
                                                jiraServiceContext.getErrorCollection());
        
        if (jiraServiceContext.getErrorCollection().hasAnyErrors()) {
            return Either.error(CommitHookHandlerError.fromErrorCollection(
                    CMD_TYPE.getName(), issue.getKey(), jiraServiceContext.getErrorCollection()));
        } else {
            return Either.value(comment);
        }

    }
}
