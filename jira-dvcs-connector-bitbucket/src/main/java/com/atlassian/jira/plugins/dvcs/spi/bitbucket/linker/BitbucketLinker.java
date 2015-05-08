package com.atlassian.jira.plugins.dvcs.spi.bitbucket.linker;

import com.atlassian.jira.plugins.dvcs.model.Repository;

import java.util.Set;


public interface BitbucketLinker
{

    /**
     * <p>Configures bitbucket repository by adding links to all JIRA projects in this JIRA instance.</p>
     * Note that current implementation makes one rest call for each project and the bitbucket is pretty slow in
     * handling those call, taking almost one second per call. If there are 100 projects this method may take 100
     * seconds.
     */
    public void linkRepository(Repository repository, Set<String> projectsInChangesets);

    /**
     * <p>Removes all links that were previously configured by {@link #linkRepository(Repository, Set)}</p>
     * Note that current implementation makes one rest call for each project and the bitbucket is pretty slow in
     * handling those call, taking almost one second per call. If there are 100 projects this method may take 100
     * seconds.
     */
    public void unlinkRepository(Repository repository);
}
