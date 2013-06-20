package com.atlassian.jira.plugins.dvcs.spi.bitbucket;

import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;

public interface BitbucketClientBuilderFactory
{

    BitbucketClientBuilder forOrganization(Organization organization);

    BitbucketClientBuilder forRepository(Repository repository);

    BitbucketClientBuilder noAuthClient(String hostUrl);

    BitbucketClientBuilder authClient(String hostUrl, String name, Credential credential);

}