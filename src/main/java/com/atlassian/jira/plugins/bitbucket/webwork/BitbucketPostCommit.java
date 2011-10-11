package com.atlassian.jira.plugins.bitbucket.webwork;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import com.atlassian.jira.plugins.bitbucket.Synchronizer;
import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryManager;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.jira.web.action.JiraWebActionSupport;

/**
 * Webwork action used to recieve the callback hook from bitbucket
 */
// TODO deprecate and replace with REST service
public class BitbucketPostCommit extends JiraWebActionSupport
{
    private final Logger logger = LoggerFactory.getLogger(BitbucketPostCommit.class);
    private final Synchronizer synchronizer;

    // Validation Error Messages
    private String validations = "";
    // Project Key
    private String projectKey = "";
    // Revision Number
    private String revision = "";
    // BitBucket JSON Payload
    private String payload = "";
	private final RepositoryManager globalRepositoryManager;
	private String repositoryUrl;

    public BitbucketPostCommit(@Qualifier("globalRepositoryManager") RepositoryManager globalRepositoryManager, 
    		Synchronizer synchronizer)
    {
        this.globalRepositoryManager = globalRepositoryManager;
		this.synchronizer = synchronizer;
    }

    protected void doValidation()
    {

        if (projectKey.equals(""))
        {
            validations += "Missing Required 'projectKey' parameter. <br/>";
        }

        if (payload.equals(""))
        {
            validations += "Missing Required 'payload' parameter. <br/>";
        }

    }

    protected String doExecute() throws Exception
    {
    	logger.debug("recieved callback post for project [ {} ]", projectKey);
    	if (validations.equals(""))
    	{
	    	if (repositoryUrl==null)
	    	{
	    		// this is most likely post from bitbucket.org
	    		JSONObject jsonPayload = new JSONObject(payload);
	    		String owner = jsonPayload.getJSONObject("repository").getString("owner");
	    		String slug = jsonPayload.getJSONObject("repository").getString("slug");
	    		repositoryUrl = "https://bitbucket.org/"+owner+"/"+slug;
	    	}
	    	
	    	List<Changeset> changesets = globalRepositoryManager.parsePayload(projectKey, repositoryUrl, payload);
			synchronizer.synchronize(projectKey, repositoryUrl, changesets);
        }

        return "postcommit";
    }

    public String getValidations()
    {
        return this.validations;
    }

    public void setProjectKey(String value)
    {
        this.projectKey = value;
    }

    public String getProjectKey()
    {
        return projectKey;
    }

    public void setRevision(String value)
    {
        this.revision = value;
    }

    public String getRevision()
    {
        return revision;
    }

    public void setPayload(String value)
    {
        this.payload = value;
    }

    public String getPayload()
    {
        return payload;
    }

}
