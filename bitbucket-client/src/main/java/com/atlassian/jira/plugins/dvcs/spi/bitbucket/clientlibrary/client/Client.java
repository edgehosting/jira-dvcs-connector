package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.OAuthProvider;
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

	private OAuthProvider provider;

	public Client()
	{
		super();
	}
	
	public Client create(OAuthProvider provider) {
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
		return null;
	}
	
	public ServicesRepositoryRemoteRestpoint getServicesRest() {
		return null;
	}
	
	public RepositoryLinkRemoteRestpoint getRepositoryLinksRest() {
		return null;
	}

}

