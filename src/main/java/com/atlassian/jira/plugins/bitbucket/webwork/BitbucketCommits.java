package com.atlassian.jira.plugins.bitbucket.webwork;

import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import sun.misc.BASE64Encoder;

import java.beans.Encoder;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BitbucketCommits {

    public String repositoryURL;
    public String projectKey;

    final PluginSettingsFactory pluginSettingsFactory;
    final Logger logger = LoggerFactory.getLogger(BitbucketCommits.class);

    public BitbucketCommits(PluginSettingsFactory pluginSettingsFactory){
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

    // Generates a URL for pulling commit messages based upon the base Repository URL
    // https://bitbucket.org/ellislab/codething/master
    private String inferCommitsURL(){
        logger.debug("BitBucketCommits.inferCommitsURL() - " + repositoryURL);
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
        logger.debug("BitbucketCommits.getCommitsList()");
        URL url;
        HttpURLConnection conn;

        BufferedReader rd;
        String line;
        String result = "";

        try {

            url = new URL(this.inferCommitsURL() + "?start=" + startNumber.toString() + "&limit=50");
            logger.debug("URL: " + url);
            logger.debug("BitbucketCommits.getCommitsList() - Commits URL - " + url);

            conn = (HttpURLConnection) url.openConnection();

            logger.debug("BitbucketCommits.getCommitsList()");
            addAuthorizationTokenToConnection(conn);

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
            logger.debug("BitbucketCommits.getCommitsList() - Malformed exception");
            //e.printStackTrace();
            if(startNumber.equals(0)){
                result = "Bitbucket Repository can't be found or incorrect credentials.";
            }


            pluginSettingsFactory.createSettingsForKey(projectKey).put("currentsync" + repositoryURL + projectKey, "complete");

        } catch (Exception e) {
            logger.debug("BitbucketCommits.getCommitsList() - End of Commits or Unauthorized");
            //e.printStackTrace();

            if(startNumber.equals(0)){
                result = "Bitbucket Repository can't be found or incorrect credentials.";
            }

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

            logger.debug("BitbucketCommits().getCommitDetails()");
            addAuthorizationTokenToConnection(conn);

            conn.setInstanceFollowRedirects(true);
            conn.setRequestMethod("GET");
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while ((line = rd.readLine()) != null) {
                result += line;
            }
            rd.close();
        }catch (MalformedURLException e){
            //e.printStackTrace();
        } catch (Exception e) {
            //e.printStackTrace();
        }

        return result;
    }

    private void addAuthorizationTokenToConnection(URLConnection connection)
    {
            String bbUserName = (String)pluginSettingsFactory.createSettingsForKey(projectKey).get("bitbucketUserName" + repositoryURL);
            String bbPassword = (String)pluginSettingsFactory.createSettingsForKey(projectKey).get("bitbucketPassword" + repositoryURL);

            if (bbUserName != "" && bbPassword != ""){
                logger.debug("BitbucketCommits() - Using Basic Auth");
                logger.debug("URL: " + repositoryURL);
                logger.debug("Username: " + bbUserName);

                Encryptor encryptor = new Encryptor(this.pluginSettingsFactory);
                byte[] ciphertext = encryptor.hexStringToByteArray(bbPassword);
                bbPassword = encryptor.decrypt(ciphertext, projectKey, repositoryURL);

                BASE64Encoder enc = new sun.misc.BASE64Encoder();
                String userpassword = bbUserName + ":" + bbPassword;
                String encodedAuthorization = enc.encode(userpassword.getBytes() );
                
                connection.setRequestProperty("Authorization", "Basic "+ encodedAuthorization);
            }
    }

    private ArrayList extractProjectKey(String message){
        Pattern projectKeyPattern = Pattern.compile("(" + this.projectKey + "-\\d*)", Pattern.CASE_INSENSITIVE);
        Matcher match = projectKeyPattern.matcher(message);

        ArrayList<String> matches = new ArrayList<String>();

        while(match.find()) {
            // Get all groups for this match
            for (int i=0; i<=match.groupCount(); i++) {
                matches.add(match.group(i));
            }
        }

        return matches;
    }

    private Integer incrementCommitCount(String commitType){

        int commitCount;

        if (pluginSettingsFactory.createSettingsForKey(projectKey).get(commitType + repositoryURL) == null){
            commitCount = 0;
        }else{
            String stringCount = (String)pluginSettingsFactory.createSettingsForKey(projectKey).get(commitType + repositoryURL);
            commitCount = Integer.parseInt(stringCount) + 1;
        }

        pluginSettingsFactory.createSettingsForKey(projectKey).put(commitType + repositoryURL, Integer.toString(commitCount));

        return commitCount;

    }

    public String syncCommits(Integer startNumber){

        Date date = new Date();
        pluginSettingsFactory.createSettingsForKey(projectKey).put("bitbucketLastSyncTime" + repositoryURL, date.toString());

        logger.debug("BitbucketCommits.syncCommits()");
        String commitsAsJSON = getCommitsList(startNumber);

        String messages = "";

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

                            ArrayList extractedIssues = extractProjectKey(message);

                            // Remove duplicate IssueIDs
                            HashSet h = new HashSet(extractedIssues);
                            extractedIssues.clear();
                            extractedIssues.addAll(h);

                            for (int j=0; j < extractedIssues.size(); ++j){
                                String issueId = (String)extractedIssues.get(j).toString().toUpperCase();
                                addCommitID(issueId, commit_id, getBranchFromURL());
                                incrementCommitCount("JIRACommitTotal");
                            }
                        }

                    }else{
                        incrementCommitCount("NonJIRACommitTotal");
                    }

                }
                logger.debug("count" + startNumber.toString());
                return messages += this.syncCommits(startNumber + 50);

            }catch (JSONException e){
                logger.debug("BitbucketCommits.syncCommits() - Exception");
                e.printStackTrace();
                pluginSettingsFactory.createSettingsForKey(projectKey).put("currentsync" + repositoryURL + projectKey, "complete");
                return "Bitbucket repository can't be found or incorrect credentials.";
            }

        }

        return messages;

    }


    private String getRepositoryURLFromCommitURL(String commitURL){

        // Changeset URL example
        // https://api.bitbucket.org/1.0/repositories/jespern/django-piston/changesets/fa57572a9acf?branch=master

        String[] arrayCommitURL = commitURL.split("/");
        String[] arrayBranch = commitURL.split("=");

        String branch = "";

        if(arrayBranch.length == 1){
            branch = "default";
        }else{
            branch = arrayBranch[1];
        }

        String repoBranchURL = "https://bitbucket.org/" + arrayCommitURL[5] + "/" + arrayCommitURL[6] + "/" + branch;
        logger.debug("bitbucketCommits.getRepositoryURLFromCommitURL() - RepoBranchURL: " + repoBranchURL);
        return repoBranchURL;
    }

    // Manages the entry of multiple Bitbucket commit id hash ids associated with an issue
    // urls look like - https://api.bitbucket.org/1.0/repositories/jespern/django-piston/changesets/fa57572a9acf?branch=master
    private void addCommitID(String issueId, String commitId, String branch){
        ArrayList<String> commitArray = new ArrayList<String>();

        // First Time Repository URL is saved
        if ((ArrayList<String>)pluginSettingsFactory.createSettingsForKey(projectKey).get("bitbucketIssueCommitArray" + issueId) != null){
            commitArray = (ArrayList<String>)pluginSettingsFactory.createSettingsForKey(projectKey).get("bitbucketIssueCommitArray" + issueId);
        }

        Boolean boolExists = false;

        for (int i=0; i < commitArray.size(); i++){
            if ((inferCommitDetailsURL() + commitId + "?branch=" + branch).equals(commitArray.get(i))){
                logger.debug("Found commit id" + commitArray.get(i));
                boolExists = true;
            }
        }

        if (!boolExists){
            logger.debug("addCommitID: Adding CommitID " + inferCommitDetailsURL() + commitId );
            commitArray.add(inferCommitDetailsURL() + commitId + "?branch=" + branch);
            addIssueId(issueId);
            pluginSettingsFactory.createSettingsForKey(projectKey).put("bitbucketIssueCommitArray" + issueId, commitArray);
        }

    }


    // Removes a specific commit_id (URL) from the saved array
    private void removeCommitID(String issueId, String URLCommitID){
        ArrayList<String> commitArray = new ArrayList<String>();

        // First Time Repository URL is saved
        if ((ArrayList<String>)pluginSettingsFactory.createSettingsForKey(projectKey).get("bitbucketIssueCommitArray" + issueId) != null){
            commitArray = (ArrayList<String>)pluginSettingsFactory.createSettingsForKey(projectKey).get("bitbucketIssueCommitArray" + issueId);
        }

        Boolean boolExists = false;
        ArrayList<String> newCommitArray = new ArrayList<String>();
        for (int i=0; i < commitArray.size(); i++){

            //logger.debug("BitbucketCommits().removeCommitID - URLCommitID: " + URLCommitID);
            //logger.debug("BitbucketCommits().removeCommitID - commitArray: " + commitArray.get(i));

            if (!URLCommitID.equals(commitArray.get(i))){
                newCommitArray.add(commitArray.get(i));
            }
        }

        pluginSettingsFactory.createSettingsForKey(projectKey).put("bitbucketIssueCommitArray" + issueId, newCommitArray);

    }



    // Manages the recording of items ids for a JIRA project + Repository Pair so that we know
    // which issues within a project have commits associated with them
    private void addIssueId(String issueId){
        ArrayList<String> idsArray = new ArrayList<String>();

        // First Time Repository URL is saved
        if ((ArrayList<String>)pluginSettingsFactory.createSettingsForKey(projectKey).get("bitbucketIssueIDs" + repositoryURL) != null){
            idsArray = (ArrayList<String>)pluginSettingsFactory.createSettingsForKey(projectKey).get("bitbucketIssueIDs" + repositoryURL);
        }

        Boolean boolExists = false;

        for (int i=0; i < idsArray.size(); i++){
            if ((issueId).equals(idsArray.get(i))){
                logger.debug("Bitbucket.addIssueId() Found existing issue id - " + idsArray.get(i));
                boolExists = true;
            }
        }

        if (!boolExists){
            logger.debug("Bitbucket.addIssueId() - " + issueId);
            idsArray.add(issueId);
            pluginSettingsFactory.createSettingsForKey(projectKey).put("bitbucketIssueIDs" + repositoryURL, idsArray);
        }

    }

    // Removes all record of issues associated with this project and repository URL
    public void removeRepositoryIssueIDs(){

        ArrayList<String> idsArray = new ArrayList<String>();
        if ((ArrayList<String>)pluginSettingsFactory.createSettingsForKey(projectKey).get("bitbucketIssueIDs" + repositoryURL) != null){
            idsArray = (ArrayList<String>)pluginSettingsFactory.createSettingsForKey(projectKey).get("bitbucketIssueIDs" + repositoryURL);
        }

        // Array of JIRA Issue IDS like ['PONE-4','PONE-10']
        for (int i=0; i < idsArray.size(); i++){
            //logger.debug("BitbucketCommits.removeRepositoryIssueIDs() - " + idsArray.get(i));

            ArrayList<String> commitIDsArray = new ArrayList<String>();
            if ((ArrayList<String>)pluginSettingsFactory.createSettingsForKey(projectKey).get("bitbucketIssueCommitArray" + idsArray.get(i)) != null){
                commitIDsArray = (ArrayList<String>)pluginSettingsFactory.createSettingsForKey(projectKey).get("bitbucketIssueCommitArray" + idsArray.get(i));

                // Array of Commit URL IDs like ['http://bitbucket.org/...']
                for (int j=0; j < commitIDsArray.size(); j++){
                    logger.debug("BitbucketCommits.removeRepositoryIssueIDs() - Commit ID: " + commitIDsArray.get(j));
                    logger.debug("BitbucketCommits.removeRepositoryIssueIDs() - " + getRepositoryURLFromCommitURL(commitIDsArray.get(j)));

                    if (repositoryURL.equals(getRepositoryURLFromCommitURL(commitIDsArray.get(j)))){
                        //logger.debug("match");
                        removeCommitID(idsArray.get(i), commitIDsArray.get(j));
                    }
                }
            }

        }

    }

}
