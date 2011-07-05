package com.atlassian.jira.plugins.bitbucket.webwork;

import com.atlassian.jira.plugins.bitbucket.bitbucket.BitbucketChangeset;
import com.atlassian.jira.plugins.bitbucket.bitbucket.BitbucketChangesetFactory;
import com.atlassian.jira.plugins.bitbucket.bitbucket.RepositoryUri;
import com.atlassian.jira.plugins.bitbucket.mapper.BitbucketMapper;
import com.atlassian.jira.plugins.bitbucket.mapper.Synchronizer;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

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
    // BitBucket Repository URL
    private String branch = "";
    // Revision Number
    private String revision = "";
    // BitBucket JSON Payload
    private String payload = "";

    public BitbucketPostCommit(Synchronizer synchronizer)
    {
        this.synchronizer = synchronizer;
    }

    protected void doValidation()
    {

        if (branch.equals(""))
        {
            validations += "Missing Required 'branch' parameter. <br/>";
        }

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
            logger.debug("recieved callback post for project [ {} ] on branch [ {} ]", projectKey, branch);

            List<BitbucketChangeset> changesets = new ArrayList<BitbucketChangeset>();
            JSONObject jsonPayload = new JSONObject(payload);

            String owner = jsonPayload.getJSONObject("repository").getString("owner");
            String slug = jsonPayload.getJSONObject("repository").getString("slug");

            JSONArray commits = jsonPayload.getJSONArray("commits");

            for (int i = 0; i < commits.length(); ++i)
                changesets.add(BitbucketChangesetFactory.parse(owner, slug, commits.getJSONObject(i)));

            synchronizer.synchronize(projectKey, RepositoryUri.parse(owner+"/"+slug+"/"+branch), changesets);
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

    public void setBranch(String value)
    {
        this.branch = value;
    }

    public String getBranch()
    {
        return branch;
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
