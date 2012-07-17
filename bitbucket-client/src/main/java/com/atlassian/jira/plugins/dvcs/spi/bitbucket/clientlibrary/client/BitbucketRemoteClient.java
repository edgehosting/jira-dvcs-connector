package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.AuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.AccountRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.ChangesetRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.GroupRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.RepositoryLinkRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.RepositoryRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.ServicesRepositoryRemoteRestpoint;

/**
 * 
 * <h3>Example of use</h3>
 * <pre>
 * RepositoryRemoteRestpoint repositoriesRest = 
 * 				new BitbucketRemoteClient( new TwoLeggedOauthProvider("https://www.bitbucket.org", "coolkey9b9...", "coolsecret040oerre....") ).getRepositoriesRest();
 *
 * List&lt;BitbucketRepository&gt; repositories = repositoriesRest.getAllRepositories("teamname");
 *		
 * <pre>
 *
 * 
 * <br /><br />
 * Created on 12.7.2012, 17:04:43
 * <br /><br />
 * 
 * @see AuthProvider
 * 
 * @author jhocman@atlassian.com
 *
 */
public class BitbucketRemoteClient
{
    public static final String BITBUCKET_URL = "https://bitbucket.org";
    
    
	private final AuthProvider provider;
	
	public BitbucketRemoteClient(AuthProvider provider)
	{
		super();
		this.provider = provider;
	}
	
	public GroupRemoteRestpoint getGroupsRest() {
		return null;
	}
	
	public ChangesetRemoteRestpoint getChangesetsRest() {
		return new ChangesetRemoteRestpoint(provider.provideRequestor());
	}
	
	public RepositoryRemoteRestpoint getRepositoriesRest() {
		return new RepositoryRemoteRestpoint(provider.provideRequestor());
	}
	
	public ServicesRepositoryRemoteRestpoint getServicesRest() {
		return new ServicesRepositoryRemoteRestpoint(provider.provideRequestor());
	}
	
	public RepositoryLinkRemoteRestpoint getRepositoryLinksRest() {
		return new RepositoryLinkRemoteRestpoint(provider.provideRequestor());
	}
    
    public AccountRemoteRestpoint getAccountRest()
    {
        return new AccountRemoteRestpoint(provider.provideRequestor());
    }

	public AuthProvider getProvider()
	{
		return provider;
	}
	
}

