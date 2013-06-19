package com.atlassian.jira.plugins.dvcs.service.remote;

import com.atlassian.jira.plugins.dvcs.model.AccountInfo;

public class DvcsCommunicatorProviderImpl implements DvcsCommunicatorProvider
{
    private final DvcsCommunicator[] dvcsCommunicators;
    
    public DvcsCommunicatorProviderImpl(CachingDvcsCommunicator... dvcsCommunicators)
    {
        this.dvcsCommunicators = dvcsCommunicators;
    }

    /**
     * {@inheritDoc}
     */
    @Override
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

    /**
     * {@inheritDoc}
     */
    @Override
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
