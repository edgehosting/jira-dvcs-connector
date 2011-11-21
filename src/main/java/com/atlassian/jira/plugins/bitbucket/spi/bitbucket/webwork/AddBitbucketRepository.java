package com.atlassian.jira.plugins.bitbucket.spi.bitbucket.webwork;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import com.atlassian.jira.plugins.bitbucket.Synchronizer;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlException;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryManager;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl.BitbucketRepositoryManager;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.ApplicationProperties;

/**
 * Webwork action used to configure the bitbucket repositories
 */
public class AddBitbucketRepository extends JiraWebActionSupport
{
    private final Logger log = LoggerFactory.getLogger(AddBitbucketRepository.class);

    private String repositoryUrl;
    private String projectKey;
    private String isPrivate;
    private String adminUsername = "";
    private String adminPassword = "";
    private String bbUsername = "";
    private String bbPassword = "";
    private String postCommitUrl;

    private final RepositoryManager globalRepositoryManager;
    private final Synchronizer synchronizer;
    private final String baseUrl;


    public AddBitbucketRepository(@Qualifier("globalRepositoryManager") RepositoryManager globalRepositoryManager, 
        Synchronizer synchronizer, ApplicationProperties applicationProperties)
    {
        this.globalRepositoryManager = globalRepositoryManager;
        this.synchronizer = synchronizer;
        this.baseUrl = applicationProperties.getBaseUrl();
    }

    @Override
    protected void doValidation()
    {
    }

    @Override
    @RequiresXsrfCheck
    protected String doExecute() throws Exception
    {
        SourceControlRepository repository;
        try
        {
            // TODO, check that username/password is correct
            repository = globalRepositoryManager.addRepository(BitbucketRepositoryManager.BITBUCKET, projectKey, repositoryUrl, bbUsername, bbPassword,
                adminUsername, adminPassword, "");
            synchronizer.synchronize(repository);
        } catch (SourceControlException e)
        {
            addErrorMessage("Failed adding the repository: ["+e.getMessage()+"]");
            log.debug("Failed adding the repository: ["+e.getMessage()+"]");
            return INPUT;
        }
        try
        {
            globalRepositoryManager.setupPostcommitHook(repository);
        } catch (SourceControlException e)
        {
            log.debug("Failed adding postcommit hook: ["+e.getMessage()+"]");
            postCommitUrl = baseUrl + "/rest/bitbucket/1.0/repository/" + repository.getId() + "/sync";
            return ERROR;
        }

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

    public String getPostCommitUrl()
    {
        return postCommitUrl;
    }

    public void setPostCommitUrl(String postCommitUrl)
    {
        this.postCommitUrl = postCommitUrl;
    }
}
