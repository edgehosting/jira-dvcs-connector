package com.atlassian.jira.plugins.bitbucket.api;

import com.atlassian.jira.plugins.bitbucket.spi.RepositoryUri;

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
	 * @return project key of the project where repository is mapped to
	 */
	String getProjectKey();
	
    /**
     * @return object which gives all necessary kinds of repository URLs (base, api, ... )
     */
    RepositoryUri getRepositoryUri();
    
    /**
     * @return the name of the repository (usually the same as last section of url)
     */
    String getRepositoryName();

    /**
     * @return type of repository (bitbucket, github, ...)
     */
    String getRepositoryType();

    
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
	
	/**
	 * @return access token foe oAuth authentication when accessing private github repository
	 */
	String getAccessToken();
}
