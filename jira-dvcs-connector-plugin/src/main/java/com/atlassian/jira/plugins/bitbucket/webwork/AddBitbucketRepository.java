package com.atlassian.jira.plugins.bitbucket.webwork;

import com.atlassian.jira.plugins.bitbucket.Synchronizer;
import com.atlassian.jira.plugins.bitbucket.api.RepositoryManager;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlException;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlException.UnauthorisedException;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.spi.impl.BitbucketRepositoryManager;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

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
            repository = globalRepositoryManager.addRepository(BitbucketRepositoryManager.BITBUCKET, projectKey, repositoryUrl, adminUsername, adminPassword, "");
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
            addErrorMessage("The username/password you provided are invalid. Make sure you entered the correct username/password and that the username has admin rights on "
                + repositoryUrl + ".<br/>" + "<br/>Then, try again.<br/><br/> [" + e.getMessage() + "]");
            
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

    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }
}
