package com.atlassian.jira.plugins.dvcs.service.remote;

import com.atlassian.jira.plugins.dvcs.model.AccountInfo;

@Deprecated
// TODO just enabled for successful plugin load
public class MockCommunicator implements DvcsCommunicator
{

	public MockCommunicator()
	{
	}
	
	@Override
	public String getDvcsType()
	{
		return null;
	}

    @Override
    public AccountInfo getAccountInfo(String hostUrl, String accountName)
    {
        return null;
    }

}
