package com.atlassian.jira.plugins.dvcs.smartcommits;

import java.util.List;

public interface SmartcommitsPayloadParser
{

	List<PayloadChangeset> parse(String payload, int repositoryId);
	
}

