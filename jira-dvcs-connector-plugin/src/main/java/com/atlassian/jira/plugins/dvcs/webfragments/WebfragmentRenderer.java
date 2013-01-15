package com.atlassian.jira.plugins.dvcs.webfragments;

import java.io.IOException;

public interface WebfragmentRenderer
{

	String renderDefaultGroupsFragment(int orgId) throws IOException;
	
	String renderGroupsFragment() throws IOException;

}
