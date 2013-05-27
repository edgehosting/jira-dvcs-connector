package com.atlassian.jira.plugins.dvcs.smartcommits;

import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;
import com.atlassian.jira.plugins.dvcs.sync.impl.DefaultSynchronizer;

public interface SmartcommitsChangesetsProcessor
{
	void startProcess(Synchronizer synchronizer);
}
