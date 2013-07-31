package com.atlassian.jira.plugins.dvcs.smartcommits;

import com.atlassian.annotations.PublicApi;
import com.atlassian.jira.plugins.dvcs.smartcommits.model.CommandsResults;
import com.atlassian.jira.plugins.dvcs.smartcommits.model.CommitCommands;

@PublicApi
public interface SmartcommitsService
{
	CommandsResults doCommands(CommitCommands commands);
}
