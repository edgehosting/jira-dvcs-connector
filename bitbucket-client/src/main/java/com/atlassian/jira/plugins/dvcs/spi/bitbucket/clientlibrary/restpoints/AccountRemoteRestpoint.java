package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.ClientUtils;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketAccount;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketRepositoriesEnvelope;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteRequestor;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteResponse;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.ResponseCallback;
import com.google.common.base.Preconditions;

/**
 * AccountRemoteRestpoint
 * 
 * @author Martin Skurla mskurla@atlassian.com
 */
public class AccountRemoteRestpoint
{
    private final RemoteRequestor requestor;
    
    public AccountRemoteRestpoint(RemoteRequestor remoteRequestor)
    {
        this.requestor = remoteRequestor;
    }

    // https://confluence.atlassian.com/display/BITBUCKET/account+Resource#accountResource-GETtheaccountprofile
    public BitbucketAccount getUser(String ownerOrEmail)
    {
        checkBlank(ownerOrEmail, "Empty username information. This is probably unassigned user.");
        String getUserUrl = URLPathFormatter.format("/users/%s", ownerOrEmail);
        
        return requestor.get(getUserUrl, null, new ResponseCallback<BitbucketAccount>()
        {
            @Override
            public BitbucketAccount onResponse(RemoteResponse response)
            {
                return ClientUtils.fromJson(response.getResponse(), BitbucketRepositoriesEnvelope.class).getUser();
            }
        });
    }

    private void checkBlank(String string, String message)
    {
        Preconditions.checkState(string != null && !string.trim().isEmpty(), message);
    }

    public BitbucketAccount getCurrentUser()
    {
        String getUserUrl = URLPathFormatter.format("/user");

        return requestor.get(getUserUrl, null, new ResponseCallback<BitbucketAccount>()
        {
            @Override
            public BitbucketAccount onResponse(RemoteResponse response)
            {
                return ClientUtils.fromJson(response.getResponse(), BitbucketRepositoriesEnvelope.class).getUser();
            }
        });
    }

    /**
     * Inviting 'userEmail' to group 'repositorySlugToInvite' of bitbucket organization repositoryOwnerToInvite.
     * 
     * @param owner
     * @param userEmail
     * @param repositoryOwnerToInvite
     * @param repositorySlugToInvite 
     */
    public void inviteUser(String owner, String userEmail, String repositoryOwnerToInvite, String repositorySlugToInvite)
    {
        String inviteUserUrl = String.format("/users/%s/invitations/%s/%s/%s",
                                                       owner,
                                                       userEmail,
                                                       repositoryOwnerToInvite,
                                                       repositorySlugToInvite);
        
        requestor.put(inviteUserUrl, null, ResponseCallback.EMPTY);
    }
}
