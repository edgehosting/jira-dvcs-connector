package com.atlassian.jira.plugins.bitbucket.webwork;

import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.config.properties.PropertiesManager;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigureBitbucketRepositories extends JiraWebActionSupport {

    final PluginSettingsFactory pluginSettingsFactory;

    public ConfigureBitbucketRepositories(PluginSettingsFactory pluginSettingsFactory){
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

    protected void doValidation() {
        //System.out.println("ConfigureRepositories - doValidation()");
        for (Enumeration e =  request.getParameterNames(); e.hasMoreElements() ;) {
            String n = (String)e.nextElement();
            String[] vals = request.getParameterValues(n);
            //validations = validations + "name " + n + ": " + vals[0];
        }

        // BitBucket URL Validation
        if (!url.equals("")){
            System.out.println("URL for Evaluation: " + url + " - NA: " + nextAction);
            if (nextAction.equals("AddRepository") || nextAction.equals("DeleteReposiory")){
                // Valid URL and URL starts with bitbucket.org domain
                Pattern p = Pattern.compile("^(https|http)://bitbucket.org/[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
                Matcher m = p.matcher(url);
                if (!m.matches()){
                    addErrorMessage("URL must be for a valid Bitbucket.org repository.");
                    validations = "URL must be for a valid Bitbucket.org repository.";
                }
            }
        }else{
            if (nextAction.equals("AddRepository") || nextAction.equals("DeleteReposiory")){
                    validations = "URL must be for a valid Bitbucket.org repository.";
            }
        }

    }

    protected String doExecute() throws Exception {
        System.out.println("NextAction: " + nextAction);

        // Remove trailing slashes from URL
        if (url.endsWith("/")){
            url = url.substring(0, url.length() - 1);
        }

        // Set all URLs to HTTPS
        if (url.startsWith("http:")){
            url = url.replaceFirst("http:","https:");
        }

        // Swap overview with 'default' (likely to be pasted in)
        if (url.endsWith("/overview")){
            url = url.substring(0, url.length() - 9);
            url = url + "/default";
        }

        // Add default branch of 'default' to URL if missing
        String[] urlArray = url.split("/");

        if(urlArray.length == 5){
            url += "/default";
        }


        if (validations.equals("")){
            if (nextAction.equals("AddRepository")){

                if (repoVisibility.equals("private")){
                    System.out.println("Private Add Repository");

                    if(bbUserName == "" || bbPassword == ""){
                        System.out.println("No BB Username or Password Given");
                    }else{
                        System.out.println("ConfigureRepositories() - Adding Private Repository Credentials");
                        //System.out.println("ConfigureRepositories() UN: " + bbUserName + " PA: " + bbPassword);

                        // Store Username and Password for later Basic Auth
                        pluginSettingsFactory.createSettingsForKey(projectKey).put("bitbucketUserName" + url, bbUserName);
                        pluginSettingsFactory.createSettingsForKey(projectKey).put("bitbucketPassword" + url, bbPassword);

                        String bbTest = (String)pluginSettingsFactory.createSettingsForKey(projectKey).get("bitbucketUserName" + url);

                        System.out.println("TEST SAVE/RETURN" + bbTest);

                        postCommitURL = "BitbucketPostCommit.jspa?projectKey=" + projectKey + "&branch=" + urlArray[urlArray.length-1];
                        addRepositoryURL();
                        nextAction = "ForceSync";
                    }

                }else{
                    System.out.println("PUBLIC Add Repository");

                    postCommitURL = "BitbucketPostCommit.jspa?projectKey=" + projectKey + "&branch=" + urlArray[urlArray.length-1];
                    System.out.println(postCommitURL);
                    addRepositoryURL();
                    nextAction = "ForceSync";
                }



            }

            if (nextAction.equals("ShowPostCommitURL")){
                  postCommitURL = "BitbucketPostCommit.jspa?projectKey=" + projectKey + "&branch=" + urlArray[urlArray.length-1];
            }

            if (nextAction.equals("DeleteRepository")){
                deleteRepositoryURL();
            }

            if (nextAction.equals("CurrentSyncStatus")){

                currentSyncPage = (String)pluginSettingsFactory.createSettingsForKey(projectKey).get("currentsync" + url + projectKey);

                nonJIRACommitTotal = (String)pluginSettingsFactory.createSettingsForKey(projectKey).get("NonJIRACommitTotal" + url);
                JIRACommitTotal = (String)pluginSettingsFactory.createSettingsForKey(projectKey).get("JIRACommitTotal" + url);

                return "syncstatus";
            }

            if (nextAction.equals("SyncRepository")){
                SyncRepository();
                return "syncmessage";
            }
        }

        return INPUT;
    }

    // Manages the entry of multiple repository URLs in a single pluginSetting Key
    private void addRepositoryURL(){
        ArrayList<String> urlArray = new ArrayList<String>();

        // First Time Repository URL is saved
        if ((ArrayList<String>)pluginSettingsFactory.createSettingsForKey(projectKey).get("bitbucketRepositoryURLArray") != null){
            urlArray = (ArrayList<String>)pluginSettingsFactory.createSettingsForKey(projectKey).get("bitbucketRepositoryURLArray");
        }

        Boolean boolExists = false;

        for (int i=0; i < urlArray.size(); i++){
            if (url.toLowerCase().equals(urlArray.get(i).toLowerCase())){
                boolExists = true;
            }
        }

        if (!boolExists){
            urlArray.add(url);
            pluginSettingsFactory.createSettingsForKey(projectKey).put("bitbucketRepositoryURLArray", urlArray);
        }

    }


    private void SyncRepository(){
        System.out.println("Starting Repository Sync");

        BitbucketCommits repositoryCommits = new BitbucketCommits(pluginSettingsFactory);
        repositoryCommits.repositoryURL = url;
        repositoryCommits.projectKey = projectKey;

        // Reset Commit count
        pluginSettingsFactory.createSettingsForKey(projectKey).put("NonJIRACommitTotal" + url, "0");
        pluginSettingsFactory.createSettingsForKey(projectKey).put("JIRACommitTotal" + url, "0");

        // Starts actual search of commits via Bitbucket API, "0" designates the 'start' parameter
        repositoryCommits.syncCommits(0);

    }

    // Removes a single Repository URL from a given Project
    private void deleteRepositoryURL(){
        ArrayList<String> urlArray = new ArrayList<String>();

        // Remove associated access key (if any) for private repos
        pluginSettingsFactory.createSettingsForKey(projectKey).put("bitbucketRepositoryAccessToken" + url, null);

        urlArray = (ArrayList<String>)pluginSettingsFactory.createSettingsForKey(projectKey).get("bitbucketRepositoryURLArray");

        for (int i=0; i < urlArray.size(); i++){
            if (url.equals(urlArray.get(i))){
                urlArray.remove(i);
            }
        }

        pluginSettingsFactory.createSettingsForKey(projectKey).put("bitbucketRepositoryURLArray", urlArray);

    }

    // JIRA Project Listing
    private ComponentManager cm = ComponentManager.getInstance();
    private List<Project> projects = cm.getProjectManager().getProjectObjects();

    public List getProjects(){
        return projects;
    }

    // Stored Repository + JIRA Projects
    public ArrayList<String> getProjectRepositories(String pKey){
        return (ArrayList<String>)pluginSettingsFactory.createSettingsForKey(pKey).get("bitbucketRepositoryURLArray");
    }

    public String getProjectName(){
        return cm.getProjectManager().getProjectObjByKey(projectKey).getName();
    }

    // Mode setting to 'single' indicates that this is administration of a single JIRA project
    // Bulk setting indicates multiple projects
    private String mode = "";
    public void setMode(String value){this.mode = value;}
    public String getMode(){return mode;}

    // BitBucket UserName
    private String bbUserName = "";
    public void setbbUserName(String value){this.bbUserName = value;}
    public String getbbUserName(){return this.bbUserName;}

    // BitBucket Password
    private String bbPassword = "";
    public void setbbPassword(String value){this.bbPassword = value;}
    public String getbbPassword(){return this.bbPassword;}

    // Repository URL
    private String url = "";
    public void setUrl(String value){this.url = value;}
    public String getURL(){return url;}

    // Post Commit URL for a specific project and repository
    private String postCommitURL = "";
    public void setPostCommitURL(String value){this.postCommitURL = value;}
    public String getPostCommitURL(){return postCommitURL;}

    // Repository Visibility
    private String repoVisibility = "";
    public void setRepoVisibility(String value){this.repoVisibility = value;}
    public String getRepoVisibility(){return repoVisibility;}

    // Project Key
    private String projectKey = "";
    public void setProjectKey(String value){this.projectKey = value;}
    public String getProjectKey(){return projectKey;}

    // Form Directive
    private String nextAction = "";
    public void setNextAction(String value){this.nextAction = value;}
    public String getNextAction(){return this.nextAction;}

    // Validation Error Messages
    private String validations = "";
    public String getValidations(){return this.validations;}

    // Confirmation Messages
    private String messages = "";
    public String getMessages(){return this.messages;}

    // Base URL
    private String baseURL = PropertiesManager.getInstance().getPropertySet().getString("jira.baseurl");
    public String getBaseURL(){return this.baseURL;}

    // Redirect URL
    private String redirectURL = "";
    public String getRedirectURL(){return this.redirectURL;}

    // Current page of commits that is being processed
    private String currentSyncPage = "";
    public String getCurrentSyncPage(){return this.currentSyncPage;}


    private String nonJIRACommitTotal = "";
    public String getNonJIRACommitTotal(){return this.nonJIRACommitTotal;}

    // Current page of commits that is being processed
    private String JIRACommitTotal = "";
    public String getJIRACommitTotal(){return this.JIRACommitTotal;}

}
