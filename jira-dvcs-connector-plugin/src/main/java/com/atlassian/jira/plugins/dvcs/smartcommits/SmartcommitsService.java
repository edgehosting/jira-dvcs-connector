package com.atlassian.jira.plugins.dvcs.smartcommits;

import java.util.List;

import javax.ws.rs.core.Response;

import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.smartcommits.model.CommitCommands;

public interface SmartcommitsService
{

	void parseAndHandleCommitCommands(List<Changeset> changesets);

	Response doCommands(CommitCommands commands);

}
