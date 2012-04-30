package com.atlassian.jira.plugins.bitbucket.spi.dvcs.webwork;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import com.atlassian.jira.plugins.bitbucket.api.RepositoryManager;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.api.Synchronizer;
import com.atlassian.jira.plugins.bitbucket.api.exception.SourceControlException;
import com.atlassian.jira.plugins.bitbucket.api.exception.SourceControlException.UnauthorisedException;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.BitbucketRepositoryManager;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.web.action.JiraWebActionSupport;

/**
 * Webwork action used to configure the bitbucket repositories
 */
public class AddBitbucketOrganization extends JiraWebActionSupport
{
	private static final long serialVersionUID = 4366205447417138381L;

	private final Logger log = LoggerFactory.getLogger(AddBitbucketOrganization.class);

    private String url;
    private String adminUsername = "";
    private String adminPassword = "";

    private final Synchronizer synchronizer;

    public AddBitbucketOrganization(Synchronizer synchronizer)
    {
        this.synchronizer = synchronizer;
    }

    @Override
    @RequiresXsrfCheck
    protected String doExecute() throws Exception
    {
        /*SourceControlRepository repository;
        try
        {*/
       
       /* } catch (UnauthorisedException e)
        {
            addErrorMessage("Failed adding the repository: ["+e.getMessage()+"]");
            log.debug("Failed adding the repository: ["+e.getMessage()+"]");
            return INPUT;
        } catch (SourceControlException e)
        {
            addErrorMessage("Failed adding the repository: ["+e.getMessage()+"]");
            log.debug("Failed adding the repository: ["+e.getMessage()+"]");
            return INPUT;
        }*/
        
        try
        {
             //globalRepositoryManager.setupPostcommitHook(repository);
        } catch (SourceControlException e)
        {
          /*  log.debug("Failed adding postcommit hook: ["+e.getMessage()+"]");
            globalRepositoryManager.removeRepository(repository.getId());
            addErrorMessage("The username/password you provided are invalid. Make sure you entered the correct username/password and that the username has admin rights on "
                + url + ".<br/>" + "<br/>Then, try again.<br/><br/> [" + e.getMessage() + "]");*/
            
            return INPUT;
        }

        return INPUT;

        //return getRedirect("ConfigureBitbucketRepositories.jspa?addedRepositoryId="+repository.getId()+"&atl_token=" + getXsrfToken());
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

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
}
