package com.atlassian.jira.plugins.dvcs.service.remote;

import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class DvcsCommunicatorProviderImpl implements DvcsCommunicatorProvider
{
    @Resource
    private CachingDvcsCommunicator[] dvcsCommunicators;

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
        return getAccountInfo(hostUrl, accountName, null);
    }

    public AccountInfo getAccountInfo(String hostUrl, String accountName, String dvcsType)
    {
        // known DVCS type
        if (dvcsType != null)
        {
            for (DvcsCommunicator dvcsCommunicator : dvcsCommunicators)
            {
                if (dvcsType.equalsIgnoreCase(dvcsCommunicator.getDvcsType()))
                {
                    return dvcsCommunicator.getAccountInfo(hostUrl, accountName);
                }
            }
            // unknown DVCS type, let the guess it anyway
        }
        else
        {
            for (DvcsCommunicator dvcsCommunicator : dvcsCommunicators)
            {
                AccountInfo accountInfo = dvcsCommunicator.getAccountInfo(hostUrl, accountName);
                if (accountInfo != null)
                {
                    return accountInfo;
                }
            }
        }
        return null;
    }
}
