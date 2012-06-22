package com.atlassian.jira.plugins.dvcs.smartcommits;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Qualifier;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.issue.worklog.Worklog;
import com.atlassian.jira.plugins.dvcs.smartcommits.handlers.CommentHandler;
import com.atlassian.jira.plugins.dvcs.smartcommits.handlers.TransitionHandler;
import com.atlassian.jira.plugins.dvcs.smartcommits.handlers.WorkLogHandler;
import com.atlassian.jira.plugins.dvcs.smartcommits.model.CommitCommands;
import com.atlassian.jira.plugins.dvcs.smartcommits.model.CommitHookErrors;
import com.atlassian.jira.plugins.dvcs.smartcommits.model.Either;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.util.I18nHelper;

public class DefaultSmartcommitsService implements SmartcommitsService
{
	private static final String NO_COMMANDS = "dvcs.smartcommits.commands.nocommands";
	private static final String BAD_COMMAND = "dvcs.smartcommits.commands.badcommand";
	private static final String UNKNOWN_ISSUE = "dvcs.smartcommits.commands.unknownissue";

	private final CacheControl NO_CACHE;

	private final TransitionHandler transitionHandler;
	private final CommentHandler commentHandler;
	private final WorkLogHandler workLogHandler;

	private final IssueManager issueManager;

	private final JiraAuthenticationContext jiraAuthenticationContext;
	private final I18nHelper i18nHelper;

	private final UserManager userManager;

	public DefaultSmartcommitsService(IssueManager issueManager,
			@Qualifier("smartcommitsTransitionsHandler") TransitionHandler transitionHandler,
			@Qualifier("smartcommitsCommentHandler") CommentHandler commentHandler,
			@Qualifier("smartcommitsWorklogHandler") WorkLogHandler workLogHandler,
			JiraAuthenticationContext jiraAuthenticationContext, UserManager userManager)
	{
		this.userManager = userManager;
		NO_CACHE = new CacheControl();
		NO_CACHE.setNoCache(true);

		this.issueManager = issueManager;
		this.transitionHandler = transitionHandler;
		this.commentHandler = commentHandler;
		this.workLogHandler = workLogHandler;
		this.jiraAuthenticationContext = jiraAuthenticationContext;
		i18nHelper = jiraAuthenticationContext.getI18nHelper();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Response doCommands(CommitCommands commands)
	{
		User user = getUser(commands.getAuthor());

		if (commands == null || commands.getCommands().isEmpty())
		{
			return formResponse(CommitHookErrors.fromSingleError("commands", "", i18nHelper.getText(NO_COMMANDS)));
		}

		CommitHookErrors errors = new CommitHookErrors();
		for (CommitCommands.CommitCommand command : commands.getCommands())
		{
			CommandType commandType = CommandType.getCommandType(command.getCommandName());

			MutableIssue issue = issueManager.getIssueObject(command.getIssueKey());
			if (issue == null)
			{
				errors.addError(command.getCommandName(), command.getIssueKey(), badIssueKey(command.getIssueKey()));
				continue;
			}

			switch (commandType)
			{
			case LOG_WORK:
				Either<CommitHookErrors, Worklog> logResult = workLogHandler.handle(user, issue,
						command.getCommandName(), command.getArguments());

				if (logResult.hasError())
				{
					errors.addErrors(logResult.getError());
				}
				break;
			case COMMENT:
				Either<CommitHookErrors, Comment> commentResult = commentHandler.handle(user, issue,
						command.getCommandName(), command.getArguments());

				if (commentResult.hasError())
				{
					errors.addErrors(commentResult.getError());
				}
				break;
			case TRANSITION:
				Either<CommitHookErrors, Issue> transitionResult = transitionHandler.handle(user, issue,
						command.getCommandName(), command.getArguments());

				if (transitionResult.hasError())
				{
					errors.addErrors(transitionResult.getError());
				}
				break;
			default:
				errors.addError(command.getCommandName(), command.getIssueKey(),
						String.format(i18nHelper.getText(BAD_COMMAND), command.getCommandName(), command.getIssueKey()));
			}
		}

		return formResponse(errors);
	}

	private Response formResponse(CommitHookErrors errors)
	{
		if (errors == null || errors.isEmpty())
		{
			return Response.ok().cacheControl(NO_CACHE).build();
		} else
		{
			return Response.status(400).entity(errors).build();
		}
	}

	private String badIssueKey(String key)
	{
		return String.format(i18nHelper.getText(UNKNOWN_ISSUE), key);
	}

	private User getUser(String username)
	{

		return userManager.getUser(username);
	}
}
