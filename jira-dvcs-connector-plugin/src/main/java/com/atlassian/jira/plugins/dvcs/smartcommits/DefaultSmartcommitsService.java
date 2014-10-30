package com.atlassian.jira.plugins.dvcs.smartcommits;

import com.atlassian.crowd.embedded.api.CrowdService;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.crowd.search.EntityDescriptor;
import com.atlassian.crowd.search.builder.QueryBuilder;
import com.atlassian.crowd.search.builder.Restriction;
import com.atlassian.crowd.search.query.entity.EntityQuery;
import com.atlassian.crowd.search.query.entity.restriction.constants.UserTermKeys;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.issue.worklog.Worklog;
import com.atlassian.jira.plugins.dvcs.smartcommits.handlers.CommentHandler;
import com.atlassian.jira.plugins.dvcs.smartcommits.handlers.TransitionHandler;
import com.atlassian.jira.plugins.dvcs.smartcommits.handlers.WorkLogHandler;
import com.atlassian.jira.plugins.dvcs.smartcommits.model.CommandsResults;
import com.atlassian.jira.plugins.dvcs.smartcommits.model.CommandsResults.CommandResult;
import com.atlassian.jira.plugins.dvcs.smartcommits.model.CommitCommands;
import com.atlassian.jira.plugins.dvcs.smartcommits.model.CommitHookHandlerError;
import com.atlassian.jira.plugins.dvcs.smartcommits.model.Either;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.ws.rs.core.CacheControl;

import static com.google.common.base.Preconditions.checkNotNull;

@ExportAsService (SmartcommitsService.class)
@Component
public class DefaultSmartcommitsService implements SmartcommitsService
{
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(DefaultSmartcommitsService.class);

    private final CacheControl NO_CACHE;

    private final TransitionHandler transitionHandler;
    private final CommentHandler commentHandler;
    private final WorkLogHandler workLogHandler;

    private final IssueManager issueManager;

    private final JiraAuthenticationContext jiraAuthenticationContext;

    private final CrowdService crowdService;

    @Autowired
    public DefaultSmartcommitsService(@ComponentImport IssueManager issueManager,
            @Qualifier ("smartcommitsTransitionsHandler") TransitionHandler transitionHandler,
            @Qualifier ("smartcommitsCommentHandler") CommentHandler commentHandler,
            @Qualifier ("smartcommitsWorklogHandler") WorkLogHandler workLogHandler,
            @ComponentImport JiraAuthenticationContext jiraAuthenticationContext,
            @ComponentImport CrowdService crowdService)
    {
		this.crowdService = checkNotNull(crowdService);

		NO_CACHE = new CacheControl();
		NO_CACHE.setNoCache(true);

		this.issueManager = checkNotNull(issueManager);
		this.transitionHandler = transitionHandler;
		this.commentHandler = commentHandler;
		this.workLogHandler = workLogHandler;
		this.jiraAuthenticationContext = checkNotNull(jiraAuthenticationContext);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public CommandsResults doCommands(CommitCommands commands)
	{
		CommandsResults results = new CommandsResults();

		//
		// recognise user and auth user by email
		//
		String authorEmail = commands.getAuthorEmail();
		String authorName = commands.getAuthorName();
		if (StringUtils.isBlank(authorEmail))
		{
            results.addGlobalError("Changeset doesn't contain author email. Unable to map this to JIRA user.");
			return results;
		}

		//
		// Fetch user by email
		//
		List<User> users = getUserByEmailOrNull(authorEmail, authorName);
		if (users.isEmpty())
		{
            results.addGlobalError("Can't find JIRA user with given author email: " + authorEmail);
			return results;
		} else if (users.size() > 1)
		{
		    results.addGlobalError("Found more than one JIRA user with email: " + authorEmail);
		    return results;
		}
		
		User user = users.get(0);

		//
		// Authenticate user
		//
		jiraAuthenticationContext.setLoggedInUser(user);

		if (CollectionUtils.isEmpty(commands.getCommands()))
		{
			results.addGlobalError("No commands to execute.");
			return results;
		}

		//
		// finally we can process commands
		//
		log.debug("Processing commands : " + commands);

		processCommands(commands, results, user);

		log.debug("Processing commands results : " + results);

		return results;
	}

	private void processCommands(CommitCommands commands, CommandsResults results, User user)
	{
		for (CommitCommands.CommitCommand command : commands.getCommands())
		{
			CommandType commandType = CommandType.getCommandType(command.getCommandName());
			//
			// init command result
			//
			CommandResult commandResult = new CommandResult();
			results.addResult(command, commandResult);

			MutableIssue issue = issueManager.getIssueObject(command.getIssueKey());
			if (issue == null)
			{
				commandResult.addError("Issue has not been found :" + command.getIssueKey());
				continue;
			}

			switch (commandType)
			{
			// -----------------------------------------------------------------------------------------------
			// Log Work
			// -----------------------------------------------------------------------------------------------
			case LOG_WORK:
				Either<CommitHookHandlerError, Worklog> logResult = workLogHandler.handle(user, issue,
						command.getCommandName(), command.getArguments(), commands.getCommitDate());

				if (logResult.hasError())
				{
					commandResult.addError(logResult.getError() + "");
				}
				break;
			// -----------------------------------------------------------------------------------------------
			// Comment
			// -----------------------------------------------------------------------------------------------
			case COMMENT:
				Either<CommitHookHandlerError, Comment> commentResult = commentHandler.handle(user, issue,
						command.getCommandName(), command.getArguments(), commands.getCommitDate());

				if (commentResult.hasError())
				{
					commandResult.addError(commentResult.getError() + "");
				}
				break;
			// -----------------------------------------------------------------------------------------------
			// Transition
			// -----------------------------------------------------------------------------------------------
			case TRANSITION:
				Either<CommitHookHandlerError, Issue> transitionResult = transitionHandler.handle(user, issue,
						command.getCommandName(), command.getArguments(), commands.getCommitDate());

				if (transitionResult.hasError())
				{
					commandResult.addError(transitionResult.getError() + "");
				}
				break;

			default:
				commandResult.addError("Invalid command " + command.getCommandName());
			}
		}
	}

	private List<User> getUserByEmailOrNull(String email, String name)
	{
		try
		{
		    List<User> users  = Lists.newArrayList();
			EntityQuery<User> query = QueryBuilder.queryFor(User.class, EntityDescriptor.user())
					.with(Restriction.on(UserTermKeys.EMAIL).exactlyMatching(email)).returningAtMost(EntityQuery.ALL_RESULTS);

			Iterable<User> user = crowdService.search(query);
			Iterator<User> iterator = user.iterator();
			User firstShouldBeOneUser = iterator.next();
			users.add(firstShouldBeOneUser);
			log.debug("Found {} by email {}", new Object [] { firstShouldBeOneUser.getName(), firstShouldBeOneUser.getEmailAddress()});

			if (iterator.hasNext())
			{
			    // try to find map user according the name
				while (iterator.hasNext())
				{
				    User nextUser = iterator.next();
				    if (nextUser.getName().equals(name))
				    {
				        return Collections.singletonList(nextUser);
				    }
				    users.add(nextUser);
				}
				log.warn("Found more than one user by email {} but no one is {}.", new Object [] { email, name });
				return users;
			}

			return Collections.singletonList(firstShouldBeOneUser);

		} catch (Exception e)
		{
			log.warn("User not found by email {}.", email);
			return Collections.EMPTY_LIST;
		}
	}
}
