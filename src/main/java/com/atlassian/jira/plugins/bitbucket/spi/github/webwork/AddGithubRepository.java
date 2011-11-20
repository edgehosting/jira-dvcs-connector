package com.atlassian.jira.plugins.bitbucket.spi.github.webwork;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import com.atlassian.jira.plugins.bitbucket.Synchronizer;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryManager;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.webwork.AddBitbucketRepository;
import com.atlassian.jira.plugins.bitbucket.spi.github.impl.GithubRepositoryManager;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.web.action.JiraWebActionSupport;

public class AddGithubRepository extends JiraWebActionSupport
{
    private final Logger log = LoggerFactory.getLogger(AddBitbucketRepository.class);

    private String repositoryUrl;
    private String projectKey;
    private String isPrivate;
    private String adminUsername = "";
    private String adminPassword = "";
    private String bbUsername = "";
    private String bbPassword = "";

    private final RepositoryManager globalRepositoryManager;

    private final Synchronizer synchronizer;

    public AddGithubRepository(@Qualifier("globalRepositoryManager") RepositoryManager globalRepositoryManager, Synchronizer synchronizer)
    {
        this.globalRepositoryManager = globalRepositoryManager;
        this.synchronizer = synchronizer;
    }

    @Override
    protected void doValidation()
    {
    }

    @Override
    @RequiresXsrfCheck
    protected String doExecute() throws Exception
    {
        SourceControlRepository repository = globalRepositoryManager.addRepository(GithubRepositoryManager.GITHUB, projectKey, repositoryUrl, bbUsername, bbPassword,
            adminUsername, adminPassword);
        synchronizer.synchronize(repository);
        globalRepositoryManager.setupPostcommitHook(repository);

        return getRedirect("ConfigureBitbucketRepositories.jspa?atl_token=" + getXsrfToken());
    }

    public String getRepositoryUrl()
    {
        return repositoryUrl;
    }

    public void setRepositoryUrl(String repositoryUrl)
    {
        this.repositoryUrl = repositoryUrl;
    }

    public String getProjectKey()
    {
        return projectKey;
    }

    public void setProjectKey(String projectKey)
    {
        this.projectKey = projectKey;
    }

    public boolean isPrivate()
    {
        return Boolean.parseBoolean(isPrivate);
    }

    public void setIsPrivate(String isPrivate)
    {
        this.isPrivate = isPrivate;
    }

    public String getAdminUsername()
    {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername)
    {
        this.adminUsername = adminUsername;
    }

    public String getAdminPassword()
    {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword)
    {
        this.adminPassword = adminPassword;
    }

    public String getBbUsername()
    {
        return bbUsername;
    }

    public void setBbUsername(String bbUsername)
    {
        this.bbUsername = bbUsername;
    }

    public String getBbPassword()
    {
        return bbPassword;
    }

    public void setBbPassword(String bbPassword)
    {
        this.bbPassword = bbPassword;
    }
}
