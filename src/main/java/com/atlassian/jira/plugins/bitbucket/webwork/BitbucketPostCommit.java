package com.atlassian.jira.plugins.bitbucket.webwork;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import com.atlassian.jira.plugins.bitbucket.bitbucket.BitbucketChangesetFactory;
import com.atlassian.jira.plugins.bitbucket.bitbucket.RepositoryUri;
import com.atlassian.jira.plugins.bitbucket.common.Changeset;
import com.atlassian.jira.plugins.bitbucket.common.RepositoryManager;
import com.atlassian.jira.plugins.bitbucket.mapper.Synchronizer;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.jira.web.action.JiraWebActionSupport;

/**
 * Webwork action used to recieve the callback hook from bitbucket
 */
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
        if (validations.equals(""))
        {
            logger.debug("recieved callback post for project [ {} ]", projectKey);
            
            List<Changeset> changesets = new ArrayList<Changeset>();
            JSONObject jsonPayload = new JSONObject(payload);

            String owner = jsonPayload.getJSONObject("repository").getString("owner");
            String slug = jsonPayload.getJSONObject("repository").getString("slug");
            RepositoryUri repositoryUri = RepositoryUri.parse(owner+"/"+slug);

            JSONArray commits = jsonPayload.getJSONArray("commits");

            for (int i = 0; i < commits.length(); ++i)
                changesets.add(BitbucketChangesetFactory.parse(repositoryUri.getRepositoryUrl(), commits.getJSONObject(i)));

			synchronizer.synchronize(projectKey, repositoryUri, changesets);
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
