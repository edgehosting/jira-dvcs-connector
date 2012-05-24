package com.atlassian.jira.plugins.bitbucket.spi.bitbucket;

import com.atlassian.jira.plugins.dvcs.model.Repository;


public interface BitbucketLinker
{
    /**
     * Add Repository Link for each project to all repositories of given organization.
     * 
     * Note that current implementation makes one rest call for each repository-project pair. If there 
     * are 20 JIRA projects and 20 Bitbucket repositories this will result in 400 rest calls and it will 
     * take around 5 mins. 
     * 
     * @param organization
     * @param projectsToLink
     */
//    void setConfiguration(Organization organization, Set<String> projectsToLink);

	public void linkRepository(Repository repository);

	public void unlinkRepository(Repository repository);
}
