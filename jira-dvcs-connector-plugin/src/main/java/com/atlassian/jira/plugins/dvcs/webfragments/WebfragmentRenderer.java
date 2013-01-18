package com.atlassian.jira.plugins.dvcs.webfragments;

import java.io.IOException;

public interface WebfragmentRenderer
{

    /**
     * Renders list of groups for default groups configuration for DVCS account 
     * 
     * @param orgId organization id
     * @return 
     * @throws IOException
     */
	String renderDefaultGroupsFragment(int orgId) throws IOException;
	
	/**
	 * Renders fragment with list of groups with checkboxes for add user dialog
	 * 
	 * @return String representation of html fragment that represents list of organizations and corresponding groups
	 * @throws IOException
	 */
	String renderGroupsFragmentForAddUser() throws IOException;

}
