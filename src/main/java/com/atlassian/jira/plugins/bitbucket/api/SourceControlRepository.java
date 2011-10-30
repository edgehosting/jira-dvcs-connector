package com.atlassian.jira.plugins.bitbucket.api;

/**
 * Interface describing the repository
 */
public interface SourceControlRepository
{
	/**
	 * @return id of the repository
	 */
	int getId();
	
	/**
	 * @return full url of the repository
	 */
	String getUrl();

	/**
	 * @return username to use for authenticating
	 */
	String getUsername();

	/**
	 * @return password to use for authenticating
	 */
	String getPassword();

	/**
	 * @return project key of the project where repository is mapped to
	 */
	String getProjectKey();
	
	/**
	 * Admin username - used when (un)installing postcommit hook
	 * @return
	 */
	String getAdminUsername();
	/**
	 * Admin password - used when (un)installing postcommit hook
	 * @return
	 */
	String getAdminPassword();
}
