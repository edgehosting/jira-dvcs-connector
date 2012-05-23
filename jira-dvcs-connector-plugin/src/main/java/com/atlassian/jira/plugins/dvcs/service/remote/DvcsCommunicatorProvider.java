package com.atlassian.jira.plugins.dvcs.service.remote;

import com.atlassian.jira.plugins.dvcs.model.AccountInfo;

public class DvcsCommunicatorProvider
{
    private final DvcsCommunicator[] dvcsCommunicators;
    
    public DvcsCommunicatorProvider(CachingDvcsCommunicator... dvcsCommunicators)
    {
        this.dvcsCommunicators = dvcsCommunicators;
    }

    public DvcsCommunicator getCommunicator(String dvcsType)
    {
        for (DvcsCommunicator dvcsCommunicator : dvcsCommunicators)
        {
            if (dvcsCommunicator.getDvcsType().equals(dvcsType))
            {
                return dvcsCommunicator;
            }
        }
        throw new IllegalArgumentException("Unsupported DVCS Type: " + dvcsType);
    }

    public AccountInfo getAccountInfo(String hostUrl, String accountName)
    {
        for (DvcsCommunicator dvcsCommunicator : dvcsCommunicators)
        {
            AccountInfo accountInfo = dvcsCommunicator.getAccountInfo(hostUrl, accountName);
            if (accountInfo != null)
            {
                return accountInfo;
            }
        }
        return null;
    }
}
