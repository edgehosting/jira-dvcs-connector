package com.atlassian.jira.plugins.bitbucket.webwork;

import com.atlassian.jira.util.json.*;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

/**
 * Created by IntelliJ IDEA.
 * User: michaelbuckbee
 * Date: 4/14/11
 * Time: 4:39 AM
 * To change this template use File | Settings | File Templates.
 */
public class BitbucketPostCommit extends JiraWebActionSupport {

    final PluginSettingsFactory pluginSettingsFactory;

    public BitbucketPostCommit(PluginSettingsFactory pluginSettingsFactory){
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

    protected void doValidation() {

        if (branch.equals("")){
            validations += "Missing Required 'branch' parameter. <br/>";
        }

        if (projectKey.equals("")){
            validations += "Missing Required 'projectKey' parameter. <br/>";
        }

    }

    protected String doExecute() throws Exception {

        if (validations.equals("")){
            System.out.println("Starting PostCommitUpdate");

            System.out.println("BB Payload - " + payload);

            JSONObject jsonPayload = new JSONObject(payload);
            JSONObject jsonRepository = jsonPayload.getJSONObject("repository");

            JSONArray jsonCommits = jsonPayload.getJSONArray("commits");

            Integer intRevision = 0;

            for (int i = 0; i < jsonCommits.length(); ++i) {
                JSONObject commit = (JSONObject)jsonCommits.get(i);
                Integer commitRevision = Integer.parseInt(commit.getString("revision"));

                if(intRevision.equals(0)){
                    intRevision = commitRevision;
                }else if(commitRevision < intRevision){
                    intRevision = commitRevision;
                }

            }

            // absolute_url element returned from JSON payload has a trailing slash
            String url = "https://bitbucket.org" + jsonRepository.getString("absolute_url") + branch;
            System.out.println("Post Commit repository URL - " + url);
            BitbucketCommits repositoryCommits = new BitbucketCommits(pluginSettingsFactory);
            repositoryCommits.repositoryURL = url;
            repositoryCommits.projectKey = projectKey;

            // 'revision' parameter will tell the api where to start searching for new commits
            validations = repositoryCommits.syncCommits(intRevision);


        }

        return "postcommit";
    }

    // Validation Error Messages
    private String validations = "";
    public String getValidations(){return this.validations;}

    // Project Key
    private String projectKey = "";
    public void setProjectKey(String value){this.projectKey = value;}
    public String getProjectKey(){return projectKey;}

    // BitBucket Repository URL
    private String branch = "";
    public void setBranch(String value){this.branch = value;}
    public String getBranch(){return branch;}

    // Revision Number
    private String revision = "";
    public void setRevision(String value){this.revision = value;}
    public String getRevision(){return revision;}

    // BitBucket JSON Payload
    private String payload = "";
    public void setPayload(String value){this.payload = value;}
    public String getPayload(){return payload;}


}
