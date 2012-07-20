package com.atlassian.jira.plugins.dvcs.spi.bitbucket.transformers;

import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketAccount;

/**
 * DvcsUserTransformer
 * 
 * @author Martin Skurla mskurla@atlassian.com
 */
public class DvcsUserTransformer {
    private DvcsUserTransformer() {}

    
    public static DvcsUser fromBitbucketAccount(BitbucketAccount bitbucketAccount)
    {
        return new DvcsUser(bitbucketAccount.getUsername(),
                            bitbucketAccount.getFirstName(),
                            bitbucketAccount.getLastName(),
                            bitbucketAccount.getAvatar());
    }
}
