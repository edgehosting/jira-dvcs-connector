package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangesetPage;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.AuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteRequestor;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteResponse;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.ResponseCallback;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.AccountRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.BranchesAndTagsRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.ChangesetRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.GroupRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.PullRequestRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.RepositoryLinkRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.RepositoryRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.ServiceRemoteRestpoint;
import com.google.gson.reflect.TypeToken;

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
    
    private final AccountRemoteRestpoint accountRemoteRestpoint;
    private final ChangesetRemoteRestpoint changesetRemoteRestpoint;
    private final GroupRemoteRestpoint groupRemoteRestpoint;
    private final RepositoryLinkRemoteRestpoint repositoryLinkRemoteRestpoint;
    private final RepositoryRemoteRestpoint repositoryRemoteRestpoint;
    private final ServiceRemoteRestpoint serviceRemoteRestpoint;

    private final RemoteRequestor requestor;

    private final BranchesAndTagsRemoteRestpoint branchesAndTagsRemoteRestpoint;
    private final PullRequestRemoteRestpoint pullRequestsEndpoint;
	
	public BitbucketRemoteClient(AuthProvider provider)
	{
        requestor = provider.provideRequestor();
        
        this.accountRemoteRestpoint = new AccountRemoteRestpoint(requestor);
        this.changesetRemoteRestpoint = new ChangesetRemoteRestpoint(requestor, new ResponseCallback<BitbucketChangesetPage>()
        {

            @Override
            public BitbucketChangesetPage onResponse(RemoteResponse response)
            {
                return ClientUtils.fromJson(response.getResponse(), new TypeToken<BitbucketChangesetPage>()
                {
                }.getType());
            }

        });
        this.groupRemoteRestpoint = new GroupRemoteRestpoint(requestor);
        this.repositoryLinkRemoteRestpoint = new RepositoryLinkRemoteRestpoint(requestor);
        this.repositoryRemoteRestpoint = new RepositoryRemoteRestpoint(requestor);
        this.serviceRemoteRestpoint = new ServiceRemoteRestpoint(requestor);
        this.branchesAndTagsRemoteRestpoint = new BranchesAndTagsRemoteRestpoint(requestor);
        this.pullRequestsEndpoint = new PullRequestRemoteRestpoint(requestor);
	}
	
    public AccountRemoteRestpoint getAccountRest()
    {
        return accountRemoteRestpoint;
    }
    
	public ChangesetRemoteRestpoint getChangesetsRest()
    {
		return changesetRemoteRestpoint;
	}
    
    public GroupRemoteRestpoint getGroupsRest()
    {
		return groupRemoteRestpoint;
	}
    
	public RepositoryLinkRemoteRestpoint getRepositoryLinksRest()
    {
		return repositoryLinkRemoteRestpoint;
	}
	
	public RepositoryRemoteRestpoint getRepositoriesRest()
    {
		return repositoryRemoteRestpoint;
	}
   
	public ServiceRemoteRestpoint getServicesRest()
    {
		return serviceRemoteRestpoint;
	}

	public BranchesAndTagsRemoteRestpoint getBranchesAndTagsRemoteRestpoint()
    {
        return branchesAndTagsRemoteRestpoint;
    }
	
	public PullRequestRemoteRestpoint getPullRequestAndCommentsRemoteRestpoint()
	{
	    return this.pullRequestsEndpoint;
	}

    public RemoteRequestor getRequestor()
    {
        return requestor;
    }
}

