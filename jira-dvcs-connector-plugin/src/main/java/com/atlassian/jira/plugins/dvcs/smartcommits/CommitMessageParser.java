package com.atlassian.jira.plugins.dvcs.smartcommits;

import com.atlassian.annotations.PublicApi;
import com.atlassian.jira.plugins.dvcs.smartcommits.model.CommitCommands;

@PublicApi
public interface CommitMessageParser
{

	/**
	 * Parse the comment
	 *
	 * @param comment The comment to parse
	 * @return The parsed actions
	 */
	CommitCommands parseCommitComment(final String comment);

}
