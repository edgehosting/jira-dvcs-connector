package com.atlassian.jira.plugins.bitbucket.webwork;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import com.atlassian.jira.plugins.bitbucket.Synchronizer;
import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.rest.RootResource;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryManager;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.jira.web.action.JiraWebActionSupport;

/**
 * Webwork action used to recieve the callback hook from bitbucket
 * Deprecated, use {@link RootResource} instead 
 */
@Deprecated 
public class BitbucketPostCommit extends JiraWebActionSupport
{
    private final Logger logger = LoggerFactory.getLogger(BitbucketPostCommit.class);
    private final Synchronizer synchronizer;

    // Validation Error Messages
    private String validations = "";
    // Project Key
    private String projectKey = "";
    private String repositoryUrl;
    private String repositoryId;
    // BitBucket JSON Payload
    private String payload = "";
	private final RepositoryManager globalRepositoryManager;

    public BitbucketPostCommit(@Qualifier("globalRepositoryManager") RepositoryManager globalRepositoryManager, 
    		Synchronizer synchronizer)
    {
        this.globalRepositoryManager = globalRepositoryManager;
		this.synchronizer = synchronizer;
    }

    @Override
    protected void doValidation()
    {

        if (StringUtils.isEmpty(projectKey) && StringUtils.isEmpty(repositoryId) )
        {
            validations += "Either 'projectKey' or 'repositoryId' parameter is required. <br/>";
        }

        if (payload.equals(""))
        {
            validations += "Missing Required 'payload' parameter. <br/>";
        }

    }

    @Override
    protected String doExecute() throws Exception
    {
		try
		{
		    log.debug("Received call to sync repo ["+repositoryId+"] with payload ["+payload+"]");
			int repoId = Integer.parseInt(repositoryId);
			SourceControlRepository repo = globalRepositoryManager.getRepository(repoId);
			List<Changeset> changesets = globalRepositoryManager.parsePayload(repo, payload);
			synchronizer.synchronize(repo, changesets);
			return "postcommit";
		} catch (NumberFormatException e)
		{
			return backwardCompabitibleDoExecute();
		}
    }

    private String backwardCompabitibleDoExecute() throws JSONException
	{
    	if (validations.equals(""))
    	{
    		logger.debug("recieved callback post for project [ {} ]", projectKey);
	    	if (repositoryUrl==null)
	    	{
	    		// this is most likely post from bitbucket.org 
	    		JSONObject jsonPayload = new JSONObject(payload);
	    		String owner = jsonPayload.getJSONObject("repository").getString("owner");
	    		String slug = jsonPayload.getJSONObject("repository").getString("slug");
	    		repositoryUrl = "https://bitbucket.org/"+owner+"/"+slug;
	    	}
	    	SourceControlRepository repo = findRepository();
	    	if (repo!=null)
	    	{
	    	    List<Changeset> changesets = globalRepositoryManager.parsePayload(repo, payload);
	    	    synchronizer.synchronize(repo, changesets);
	    	}
        }

        return "postcommit";
	}

    private SourceControlRepository findRepository()
    {
        List<SourceControlRepository> repositories = globalRepositoryManager.getRepositories(projectKey);
        for (SourceControlRepository repo : repositories)
        {
            if (repositoryUrl.equals(repo.getRepositoryUri().getRepositoryUrl()))
            {
                return repo;
            }
        }
        return null;
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

    public void setPayload(String value)
    {
        this.payload = value;
    }

    public String getPayload()
    {
        return payload;
    }

}
