package com.atlassian.jira.plugins.dvcs.webfragments;

import java.io.IOException;

public interface WebfragmentRenderer
{
	
	/**
	 * Renders fragment with list of groups with checkboxes for add user dialog
	 * 
	 * @return String representation of html fragment that represents list of organizations and corresponding groups
	 * @throws IOException
	 */
	String renderGroupsFragmentForAddUser() throws IOException;

}
