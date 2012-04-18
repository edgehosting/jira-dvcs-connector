package com.atlassian.jira.plugins.bitbucket.spi.github.impl;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.plugins.bitbucket.IssueLinker;
import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.Communicator;
import com.atlassian.jira.plugins.bitbucket.api.Encryptor;
import com.atlassian.jira.plugins.bitbucket.api.RepositoryPersister;
import com.atlassian.jira.plugins.bitbucket.api.RepositoryUri;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlException;
import com.atlassian.jira.plugins.bitbucket.api.UrlInfo;
import com.atlassian.jira.plugins.bitbucket.spi.DvcsRepositoryManager;
import com.atlassian.jira.plugins.bitbucket.spi.github.GithubOAuth;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.templaterenderer.TemplateRenderer;

public class GithubRepositoryManager extends DvcsRepositoryManager
{
    private static final Logger log = LoggerFactory.getLogger(GithubRepositoryManager.class);

    public static final String GITHUB = "github";

    private final GithubOAuth githubOAuth;

    public GithubRepositoryManager(RepositoryPersister repositoryPersister, @Qualifier("githubCommunicator") Communicator communicator,
                                   Encryptor encryptor, ApplicationProperties applicationProperties, IssueLinker issueLinker,
                                   TemplateRenderer templateRenderer, IssueManager issueManager, GithubOAuth githubOAuth)
    {
        super(communicator, repositoryPersister, encryptor, applicationProperties, issueLinker, templateRenderer, issueManager);
        this.githubOAuth = githubOAuth;
    }


    @Override
    public Changeset reloadChangeset(int repositoryId, String node, String issueId, String branch)
    {
        Changeset reloadedChangeset = super.reloadChangeset(repositoryId, node, issueId, branch);
        if (StringUtils.isNotBlank(branch))
        {
            reloadedChangeset.setBranch(branch);
        }

        return reloadedChangeset;
    }


    @Override
    public String getRepositoryType()
    {
        return GITHUB;
    }
    
    @Override
    public UrlInfo validateUrlInfo(UrlInfo urlInfo)
    {
        urlInfo = super.validateUrlInfo(urlInfo);
        if (StringUtils.isBlank(githubOAuth.getClientId()) || StringUtils.isBlank(githubOAuth.getClientSecret()))
        {
            String baseUrl = getApplicationProperties().getBaseUrl();
            urlInfo.addValidationError("<a href='"+baseUrl+"/secure/admin/ConfigureGithubOAuth!default.jspa'>GitHub OAuth Settings</a> have to be configured before adding GitHub repository");
        }
        return urlInfo;
    }

    @Override
    public RepositoryUri getRepositoryUri(String urlString)
    {
        try
        {
            URL url = new URL(urlString);
            String protocol = url.getProtocol();
            String hostname = url.getHost();
            String path = url.getPath();
            String[] split = StringUtils.split(path, "/");
            if (split.length < 2)
            {
                throw new SourceControlException("Expected url is https://domainname.com/username/repository");
            }
            String owner = split[0];
            String slug = split[1];
            return new GithubRepositoryUri(protocol, hostname, owner, slug);
        } catch (MalformedURLException e)
        {
            throw new SourceControlException("Invalid url [" + urlString + "]");
        }
    }
}
