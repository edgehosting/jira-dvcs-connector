package com.atlassian.jira.plugins.dvcs.smartcommits;

import javax.ws.rs.core.Response;

import com.atlassian.jira.plugins.dvcs.smartcommits.model.CommitCommands;

public interface SmartcommitsService
{

	Response doCommands(CommitCommands commands);

}
