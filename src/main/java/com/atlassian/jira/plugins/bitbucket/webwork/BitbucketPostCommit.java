package com.atlassian.jira.plugins.bitbucket.webwork;

import com.atlassian.jira.plugins.bitbucket.mapper.Synchronizer;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            System.out.println("Starting PostCommitUpdate");
            System.out.println("BB Payload - " + payload);

            JSONObject jsonPayload = new JSONObject(payload);
            JSONObject jsonRepository = jsonPayload.getJSONObject("repository");

            // absolute_url element returned from JSON payload has a trailing slash
            String url = "https://bitbucket.org" + jsonRepository.getString("absolute_url") + branch;
            System.out.println("Post Commit repository URL - " + url);

            synchronizer.postReceiveHook(payload);
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
