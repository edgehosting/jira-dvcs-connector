package com.atlassian.jira.plugins.dvcs.smartcommits.handlers;

import com.atlassian.jira.bc.issue.comment.CommentService;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.plugins.dvcs.smartcommits.CommandType;
import com.atlassian.jira.plugins.dvcs.smartcommits.model.CommitHookHandlerError;
import com.atlassian.jira.plugins.dvcs.smartcommits.model.Either;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

@ExportAsService (CommandHandler.class)
@Component ("smartcommitsCommentHandler")
public class CommentHandler implements CommandHandler<Comment>
{

    private static CommandType CMD_TYPE = CommandType.COMMENT;

    private final CommentService commentService;

    @Autowired
    @SuppressWarnings ("SpringJavaAutowiringInspection")
    public CommentHandler(@ComponentImport CommentService commentService)
    {
        this.commentService = checkNotNull(commentService);
    }

    @Override
    public CommandType getCommandType()
    {
        return CMD_TYPE;
    }

    @Override
    public Either<CommitHookHandlerError, Comment> handle(ApplicationUser user, MutableIssue issue, String commandName, List<String> args, Date commitDate)
    {

        final CommentService.CommentParameters commentParameters = CommentService.CommentParameters.builder()
                .issue(issue)
                .body(args.isEmpty() ? null : args.get(0))
                .created(commitDate)
                .build();
        final CommentService.CommentCreateValidationResult validationResult = commentService.validateCommentCreate(user, commentParameters);

        if (validationResult.isValid())
        {
            return Either.value(commentService.create(user, validationResult, true));
        }
        else
        {
            return Either.error(CommitHookHandlerError.fromErrorCollection(
                    CMD_TYPE.getName(), issue.getKey(), validationResult.getErrorCollection()));
        }
    }
}
