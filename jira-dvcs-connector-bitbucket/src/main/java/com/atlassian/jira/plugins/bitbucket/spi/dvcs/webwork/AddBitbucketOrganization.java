package com.atlassian.jira.plugins.bitbucket.spi.dvcs.webwork;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.bitbucket.api.Synchronizer;
import com.atlassian.jira.plugins.bitbucket.api.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.webwork.CommonDvcsConfigurationAction;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;

/**
 * Webwork action used to configure the bitbucket repositories
 */
public class AddBitbucketOrganization extends CommonDvcsConfigurationAction
{
	private static final long serialVersionUID = 4366205447417138381L;

	private final Logger log = LoggerFactory.getLogger(AddBitbucketOrganization.class);

    private String url;
    private String organization = "";
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
    
    @Override
    protected void doValidation() {
    	// TODO Auto-generated method stub
    	super.doValidation();
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

	public String getOrganization() {
		return organization;
	}

	public void setOrganization(String organization) {
		this.organization = organization;
	}
}
