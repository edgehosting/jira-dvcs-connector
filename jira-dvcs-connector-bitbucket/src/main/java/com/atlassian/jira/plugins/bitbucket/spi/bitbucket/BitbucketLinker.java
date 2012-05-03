package com.atlassian.jira.plugins.bitbucket.spi.bitbucket;

import java.util.Set;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.OrganizationMapping;

public interface BitbucketLinker
{
    /**
     * Add Repository Link for each project to all repositories of given organization.
     * 
     * Note that current implementation makes one rest call for each repository-project pair. If there 
     * are 20 JIRA projects and 20 Bitbucket repositories this will result in 400 rest calls and it will 
     * take around 5 mins. 
     * 
     * @param om
     * @param projectsToLink
     */
    void setConfiguration(OrganizationMapping om, Set<String> projectsToLink);
}
