package com.atlassian.jira.plugins.dvcs.service.remote;

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

}
