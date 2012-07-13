package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.AuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.ChangesetRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.GroupRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.RepositoryLinkRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.RepositoryRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.ServicesRepositoryRemoteRestpoint;

/**
 * TODO rename it
 * Client
 *
 * 
 * <br /><br />
 * Created on 12.7.2012, 17:04:43
 * <br /><br />
 * @author jhocman@atlassian.com
 *
 */
public class Client
{

	private AuthProvider provider;
	private String apiUrl;

	public Client()
	{
		super();
	}
	
	public Client configure(AuthProvider provider) {
		this.provider = provider;
		return this;
	}
	
	public GroupRemoteRestpoint getGroupsRest() {
		return null;
	}
	
	public ChangesetRemoteRestpoint getChangesetsRest() {
		return null;
	}
	
	public RepositoryRemoteRestpoint getRepositoriesRest() {
		return new RepositoryRemoteRestpoint (provider.provideRequestor());
	}
	
	public ServicesRepositoryRemoteRestpoint getServicesRest() {
		return null;
	}
	
	public RepositoryLinkRemoteRestpoint getRepositoryLinksRest() {
		return null;
	}

	public AuthProvider getProvider()
	{
		return provider;
	}

	public String getApiUrl()
	{
		return apiUrl;
	}
	

}

