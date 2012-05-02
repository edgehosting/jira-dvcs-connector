package com.atlassian.jira.plugins.dvcs.service.remote;

public class DvcsCommunicatorProvider
{
    private final DvcsCommunicator[] dvcsCommunicators;

    public DvcsCommunicatorProvider(DvcsCommunicator... dvcsCommunicators)
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
}
