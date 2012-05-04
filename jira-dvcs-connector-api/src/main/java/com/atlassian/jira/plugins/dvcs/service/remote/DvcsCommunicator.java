package com.atlassian.jira.plugins.dvcs.service.remote;

import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;

import java.util.List;

/**
 * Starting point for remote API calls to the bitbucket remote API
 */
public interface DvcsCommunicator
{
    String getDvcsType();

    AccountInfo getAccountInfo(String hostUrl, String accountName);

    List<Repository> getRepositories(Organization organization);
}
