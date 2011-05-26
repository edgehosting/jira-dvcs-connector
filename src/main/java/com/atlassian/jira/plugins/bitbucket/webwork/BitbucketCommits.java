package com.atlassian.jira.plugins.bitbucket.webwork;

import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import sun.misc.BASE64Encoder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BitbucketCommits {

    public String repositoryURL;
    public String projectKey;

    final PluginSettingsFactory pluginSettingsFactory;

    public BitbucketCommits(PluginSettingsFactory pluginSettingsFactory){
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

    // Generates a URL for pulling commit messages based upon the base Repository URL
    // https://bitbucket.org/ellislab/codething/master
    private String inferCommitsURL(){
        String[] path = repositoryURL.split("/");
        return "https://api.bitbucket.org/1.0/repositories/" + path[3] + "/" + path[4] + "/changesets";
    }

    // Generate a URL for pulling a single commits details (diff and author)
    private String inferCommitDetailsURL(){
        String[] path = repositoryURL.split("/");
        return "https://api.bitbucket.org/1.0/repositories/" + path[3] + "/" + path[4] + "/changesets/";
    }

    private String getBranchFromURL(){
        String[] path = repositoryURL.split("/");
        return path[5];
    }

    private String getCommitsList(Integer startNumber){
        System.out.println("BitbucketCommits.getCommitsList()");
        URL url;
        HttpURLConnection conn;

        BufferedReader rd;
        String line;
        String result = "";

        try {

            System.out.println("Commits URL - " + this.inferCommitsURL() + "?start=" + startNumber.toString() + "&limit=50");
            url = new URL(this.inferCommitsURL() + "?start=" + startNumber.toString() + "&limit=50");
            System.out.println("URL: " + url);
            conn = (HttpURLConnection) url.openConnection();

            String bbUserName = (String)pluginSettingsFactory.createSettingsForKey(projectKey).get("bitbucketUserName" + repositoryURL);
            String bbPassword = (String)pluginSettingsFactory.createSettingsForKey(projectKey).get("bitbucketPassword" + repositoryURL);

            if (bbUserName != "" && bbPassword != ""){
                System.out.println("Using Basic Auth");
                //System.out.println("URL: " + repositoryURL);
                System.out.println("UN: " + bbUserName + " PA: " + bbPassword);

                BASE64Encoder enc = new sun.misc.BASE64Encoder();
                String userpassword = bbUserName + ":" + bbPassword;
                String encodedAuthorization = enc.encode(userpassword.getBytes() );
                conn.setRequestProperty("Authorization", "Basic "+ encodedAuthorization);
            }

            conn.setInstanceFollowRedirects(true);
            conn.setRequestMethod("GET");
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while ((line = rd.readLine()) != null) {
                result += line;
            }
            rd.close();

            // Sets current page status for UI feedback
            pluginSettingsFactory.createSettingsForKey(projectKey).put("currentsync" + repositoryURL + projectKey, startNumber.toString());

        }catch (MalformedURLException e){
            //e.printStackTrace();
            System.out.println("Malformed exception");
            pluginSettingsFactory.createSettingsForKey(projectKey).put("currentsync" + repositoryURL + projectKey, "complete");

        } catch (Exception e) {
            //System.out.println("End of Commits found (500) or Unauthorized (401)");
            System.out.println("End of Commits or Unauthorized");
            pluginSettingsFactory.createSettingsForKey(projectKey).put("currentsync" + repositoryURL + projectKey, "complete");

        }

        return result;
    }

    // Commit list returns id (hashed) and Message
    // you have to call each individual commit to get diff details
    public String getCommitDetails(String commit_id_url){
        URL url;
        HttpURLConnection conn;

        BufferedReader rd;
        String line;
        String result = "";
        try {
            url = new URL(commit_id_url);
            conn = (HttpURLConnection) url.openConnection();

            String bbUserName = (String)pluginSettingsFactory.createSettingsForKey(projectKey).get("bitbucketUserName" + repositoryURL);
            String bbPassword = (String)pluginSettingsFactory.createSettingsForKey(projectKey).get("bitbucketPassword" + repositoryURL);

            if (bbUserName != "" && bbPassword != ""){
                System.out.println("BitbucketCommits().getCommitsList() - Using Basic Auth");
                System.out.println("URL: " + repositoryURL);
                System.out.println("UN: " + bbUserName + " PA: " + bbPassword);

                BASE64Encoder enc = new sun.misc.BASE64Encoder();
                String userpassword = bbUserName + ":" + bbPassword;

                String encodedAuthorization = enc.encode(userpassword.getBytes() );
                conn.setRequestProperty("Authorization", "Basic "+ encodedAuthorization);
            }

            conn.setInstanceFollowRedirects(true);
            conn.setRequestMethod("GET");
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while ((line = rd.readLine()) != null) {
                result += line;
            }
            rd.close();
        }catch (MalformedURLException e){
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    private String extractProjectKey(String message){
        Pattern projectKeyPattern = Pattern.compile("(" + this.projectKey + "-\\d*)");
        Matcher match = projectKeyPattern.matcher(message);
        Boolean boolFound = match.find();

        if(boolFound){
            return match.group(0);
        }else{
            return "";
        }
    }

    private Integer incrementCommitCount(String commitType){

        int commitCount;

        if (pluginSettingsFactory.createSettingsForKey(projectKey).get(commitType + repositoryURL) == null){
            commitCount = 0;
        }else{
            String stringCount = (String)pluginSettingsFactory.createSettingsForKey(projectKey).get(commitType + repositoryURL);
            commitCount = Integer.parseInt(stringCount) + 1;
        }

        commitCount = commitCount + 1;

        pluginSettingsFactory.createSettingsForKey(projectKey).put(commitType + repositoryURL, Integer.toString(commitCount));

        return commitCount;

    }

    public String syncCommits(Integer startNumber){

        Date date = new Date();
        pluginSettingsFactory.createSettingsForKey(projectKey).put("bitbucketLastSyncTime" + repositoryURL, date.toString());

        System.out.println("BitbucketCommits.syncCommits()");
        String commitsAsJSON = getCommitsList(startNumber);

        String messages = "";

        Integer nonJIRACommits = 0;
        Integer JIRACommits = 0;

        if (commitsAsJSON != ""){

            try{
                JSONObject jsonCommits = new JSONObject(commitsAsJSON);
                JSONArray commits = jsonCommits.getJSONArray("changesets");

                for (int i = 0; i < commits.length(); ++i) {
                    String message = commits.getJSONObject(i).getString("message");
                    String commit_id = commits.getJSONObject(i).getString("node");

                    // Detect presence of JIRA Issue Key
                    if (message.indexOf(this.projectKey) > -1){
                        if (!extractProjectKey(message).equals("")){

                            String issueId = extractProjectKey(message);
                            addCommitID(issueId, commit_id, getBranchFromURL());
                            incrementCommitCount("JIRACommitTotal");

                            JIRACommits++;

                            messages += "<div class='jira_issue'>" + issueId + " " + commit_id + "</div>";

                        }

                    }else{
                        incrementCommitCount("NonJIRACommitTotal");
                        nonJIRACommits++;
                        messages += "<div class='no_issue'>No Issue: " + commit_id + "</div>" ;
                    }

                }
                System.out.println("count" + startNumber.toString());
                return messages += this.syncCommits(startNumber + 50);

            }catch (JSONException e){
                e.printStackTrace();
                return "exception";
            }

        }else{

            // ToDo: Fix summary header
            String messageHeader = "<h2>Sync Summary</h2>";
            messageHeader += "<strong>Non JIRA Commits Found: </strong>" + nonJIRACommits.toString() + "<br/>";
            messageHeader += "<strong>JIRA Commits Found: </strong>" + JIRACommits.toString() + "<br/><p/>";

        }

        return "";

    }


    // Manages the entry of multiple BitBucket commit id hash ids associated with an issue
    // urls look like - https://api.bitbucket.org/1.0/repositories/ellislab/codeigniter/changesets/87d1266bcede?branch=master
    private void addCommitID(String issueId, String commitId, String branch){
        ArrayList<String> commitArray = new ArrayList<String>();

        // First Time Repository URL is saved
        if ((ArrayList<String>)pluginSettingsFactory.createSettingsForKey(projectKey).get("bitbucketIssueCommitArray" + issueId) != null){
            commitArray = (ArrayList<String>)pluginSettingsFactory.createSettingsForKey(projectKey).get("bitbucketIssueCommitArray" + issueId);
        }

        Boolean boolExists = false;

        for (int i=0; i < commitArray.size(); i++){
            if ((inferCommitDetailsURL() + commitId + "?branch=" + branch).equals(commitArray.get(i))){
                System.out.println("Found commit id" + commitArray.get(i));
                boolExists = true;
            }
        }

        if (!boolExists){
            System.out.println("addCommitID: Adding CommitID " + inferCommitDetailsURL() + commitId );
            commitArray.add(inferCommitDetailsURL() + commitId + "?branch=" + branch);
            pluginSettingsFactory.createSettingsForKey(projectKey).put("bitbucketIssueCommitArray" + issueId, commitArray);
        }else{
            System.out.println("addCommitID: commit id already present");
        }

        System.out.println("arrayKey: " + "bitbucketIssueCommitArray" + issueId);
        //System.out.println("addCommitID: " + issueId + " - " + commitId);

    }

    // Removes all of the associated commits from an issue
    private void deleteCommitId(String issueId){
        pluginSettingsFactory.createSettingsForKey(projectKey).put("bitbucketIssueCommitArray" + issueId, null);
    }

}
