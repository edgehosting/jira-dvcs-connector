package com.atlassian.jira.plugins.dvcs.service.remote;

import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;

import java.util.Date;
import java.util.List;

/**
 * Starting point for remote API calls to the bitbucket remote API
 */
public interface DvcsCommunicator
{
    String getDvcsType();

    AccountInfo getAccountInfo(String hostUrl, String accountName);

    List<Repository> getRepositories(Organization organization);

    Changeset getDetailChangeset(Repository repository, Changeset changeset);

    public Iterable<Changeset> getChangesets(Repository repository, Date lastCommitDate);

    public void setupPostcommitHook(Repository repository, String postCommitUrl);

    public void removePostcommitHook(Repository repository, String postCommitUrl);


}
