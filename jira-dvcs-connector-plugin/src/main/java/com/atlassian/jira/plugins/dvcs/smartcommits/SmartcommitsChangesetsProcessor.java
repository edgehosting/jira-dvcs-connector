package com.atlassian.jira.plugins.dvcs.smartcommits;

public interface SmartcommitsChangesetsProcessor
{

	void queue(SmartcommitOperation operation);

}
