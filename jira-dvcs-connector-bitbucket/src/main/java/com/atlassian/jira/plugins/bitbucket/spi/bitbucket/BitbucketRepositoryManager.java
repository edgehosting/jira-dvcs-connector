package com.atlassian.jira.plugins.bitbucket.spi.bitbucket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.plugins.bitbucket.api.Communicator;
import com.atlassian.jira.plugins.bitbucket.api.Encryptor;
import com.atlassian.jira.plugins.bitbucket.api.IssueLinker;
import com.atlassian.jira.plugins.bitbucket.api.RepositoryPersister;
import com.atlassian.jira.plugins.bitbucket.api.RepositoryUri;
import com.atlassian.jira.plugins.bitbucket.api.impl.DefaultRepositoryUri;
import com.atlassian.jira.plugins.bitbucket.api.impl.DvcsRepositoryManager;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.templaterenderer.TemplateRenderer;

public class BitbucketRepositoryManager extends DvcsRepositoryManager
{
    public static final String BITBUCKET = "bitbucket";
    private final Logger log = LoggerFactory.getLogger(BitbucketRepositoryManager.class);

    public BitbucketRepositoryManager(RepositoryPersister repositoryPersister, @Qualifier("bitbucketCommunicator") Communicator communicator,
                                      Encryptor encryptor, ApplicationProperties applicationProperties, IssueLinker issueLinker,
                                      TemplateRenderer templateRenderer, IssueManager issueManager)
    {
        super(communicator, repositoryPersister, encryptor, applicationProperties, issueLinker, templateRenderer, issueManager);
    }

    @Override
    public String getRepositoryType()
    {
        return BITBUCKET;
    }

    @Override
    public RepositoryUri getRepositoryUri(String urlString)
    {
        DefaultRepositoryUri r = DvcsRepositoryManager.parseRepositoryUri(urlString);
        return new BitbucketRepositoryUri(r.getProtocol(), r.getHostname(), r.getOwner(), r.getSlug());
    }

}
