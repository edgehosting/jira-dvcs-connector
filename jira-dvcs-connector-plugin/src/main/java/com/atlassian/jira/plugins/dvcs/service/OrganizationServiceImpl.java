package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;

import java.util.ArrayList;
import java.util.List;

public class OrganizationServiceImpl implements OrganizationService
{

    private DvcsCommunicatorProvider dvcsCommunicatorProvider;

    public OrganizationServiceImpl(DvcsCommunicatorProvider dvcsCommunicatorProvider)
    {
        this.dvcsCommunicatorProvider = dvcsCommunicatorProvider;
    }

    @Override
    public AccountInfo getAccountInfo(String hostUrl, String accountName)
    {
        return null;
    }

    @Override
    public List<Organization> getAll()
    {
        return new ArrayList<Organization>();
    }

    @Override
    public Organization get(int organizationId)
    {
        return null;
    }

    @Override
    public Organization save(Organization organization)
    {
        return null;
    }

    @Override
    public void remove(int organizationId)
    {
    }
}
