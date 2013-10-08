package com.atlassian.jira.plugins.dvcs.service.remote;

import com.atlassian.jira.plugins.dvcs.model.AccountInfo;

public interface DvcsCommunicatorProvider
{

    public abstract DvcsCommunicator getCommunicator(String dvcsType);

    public abstract AccountInfo getAccountInfo(String hostUrl, String accountName);

    public abstract AccountInfo getAccountInfo(String hostUrl, String accountName, String dvcsType);

}