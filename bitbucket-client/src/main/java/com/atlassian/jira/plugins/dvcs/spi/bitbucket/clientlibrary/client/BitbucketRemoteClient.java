package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangesetPage;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.AuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BasicAuthAuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.HttpClientProvider;
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
 * </pre>
 * 
 * @see AuthProvider
 * 
 * @author jhocman@atlassian.com
 *
 */
public class BitbucketRemoteClient
{
    public static final String BITBUCKET_URL = "https://bitbucket.org";
    public static final String TEST_USER_AGENT = "jirabitbucketconnectortest";

    private AccountRemoteRestpoint accountRemoteRestpoint;
    private ChangesetRemoteRestpoint changesetRemoteRestpoint;
    private GroupRemoteRestpoint groupRemoteRestpoint;
    private RepositoryLinkRemoteRestpoint repositoryLinkRemoteRestpoint;
    private RepositoryRemoteRestpoint repositoryRemoteRestpoint;
    private ServiceRemoteRestpoint serviceRemoteRestpoint;
    private BranchesAndTagsRemoteRestpoint branchesAndTagsRemoteRestpoint;
    private PullRequestRemoteRestpoint pullRequestsEndpoint;

    private RemoteRequestor requestor;

    public BitbucketRemoteClient(String username, String password) {

        HttpClientProvider httpClientProvider = new HttpClientProvider();
        httpClientProvider.setUserAgent(TEST_USER_AGENT);

        // Bitbucket client setup
        AuthProvider authProvider = new BasicAuthAuthProvider(BitbucketRemoteClient.BITBUCKET_URL,
                username,
                password,
                httpClientProvider);

        setup(authProvider);
    }
	
	public BitbucketRemoteClient(AuthProvider provider)
	{
        setup(provider);
	}

    private void setup(AuthProvider provider){
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

