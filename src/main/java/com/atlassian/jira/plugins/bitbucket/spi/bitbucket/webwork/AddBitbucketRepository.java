package com.atlassian.jira.plugins.bitbucket.spi.bitbucket.webwork;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import com.atlassian.jira.plugins.bitbucket.Synchronizer;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlException;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlException.UnauthorisedException;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryManager;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl.BitbucketRepositoryManager;
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
    private String bbUsername = "";
    private String bbPassword = "";

    private final RepositoryManager globalRepositoryManager;
    private final Synchronizer synchronizer;


    public AddBitbucketRepository(@Qualifier("globalRepositoryManager") RepositoryManager globalRepositoryManager, 
        Synchronizer synchronizer)
    {
        this.globalRepositoryManager = globalRepositoryManager;
        this.synchronizer = synchronizer;
    }

    @Override
    @RequiresXsrfCheck
    protected String doExecute() throws Exception
    {
        SourceControlRepository repository;
        try
        {
            repository = globalRepositoryManager.addRepository(BitbucketRepositoryManager.BITBUCKET, projectKey, repositoryUrl, bbUsername, bbPassword,
                "", "", "");
            synchronizer.synchronize(repository);
        } catch (UnauthorisedException e)
        {
            addErrorMessage("Failed adding the repository: ["+e.getMessage()+"]");
            log.debug("Failed adding the repository: ["+e.getMessage()+"]");
            return INPUT;
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
            globalRepositoryManager.removeRepository(repository.getId());
            addErrorMessage("Error adding postcommit hook. Repository was not added. Do you have admin rights to the repository? ["+e.getMessage()+"]");
            return INPUT;
        }

        return getRedirect("ConfigureBitbucketRepositories.jspa?addedRepositoryId="+repository.getId()+"&atl_token=" + getXsrfToken());
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
