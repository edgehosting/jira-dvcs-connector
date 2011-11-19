package com.atlassian.jira.plugins.bitbucket.webwork;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import com.atlassian.jira.plugins.bitbucket.Synchronizer;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryManager;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.web.action.JiraWebActionSupport;

/**
 * Webwork action used to configure the bitbucket repositories
 */
public class AddBitbucketRepository extends JiraWebActionSupport
{
    private final Logger log = LoggerFactory.getLogger(AddBitbucketRepository.class);

    private String repositoryUrl;
    private String projectKey;
    private String isPrivate;
    private String adminUsername;
    private String adminPassword;
    private String bbUsername;
    private String bbPassword;

    private final RepositoryManager globalRepositoryManager;

    private final Synchronizer synchronizer;

    public AddBitbucketRepository(@Qualifier("globalRepositoryManager") RepositoryManager globalRepositoryManager, Synchronizer synchronizer)
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
        SourceControlRepository repository = globalRepositoryManager.addRepository(projectKey, repositoryUrl, bbUsername, bbPassword,
            adminUsername, adminPassword);
        globalRepositoryManager.setupPostcommitHook(repository);
        synchronizer.synchronize(repository);

//        postCommitUrl = baseUrl + "/rest/bitbucket/1.0/repository/" + repositoryId + "/sync";

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

    public String getIsPrivate()
    {
        return isPrivate;
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
