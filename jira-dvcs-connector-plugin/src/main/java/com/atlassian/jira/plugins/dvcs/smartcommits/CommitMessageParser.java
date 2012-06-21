package com.atlassian.jira.plugins.dvcs.smartcommits;

import com.atlassian.jira.plugins.dvcs.smartcommits.model.CommitCommands;

public interface CommitMessageParser
{

	/**
	 * Parse the comment
	 *
	 * @param comment The comment to parse
	 * @return The parsed actions
	 */
	public abstract CommitCommands parseCommitComment(final String comment);

}
