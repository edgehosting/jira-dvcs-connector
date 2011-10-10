package com.atlassian.jira.plugins.bitbucket.spi;

import com.atlassian.jira.plugins.bitbucket.api.OperationResult;

public interface SynchronisationOperation
{
	OperationResult synchronise() throws Exception;
}
