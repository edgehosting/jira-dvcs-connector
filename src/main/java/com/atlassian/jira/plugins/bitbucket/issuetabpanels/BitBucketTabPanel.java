package com.atlassian.jira.plugins.bitbucket.issuetabpanels;

import com.atlassian.core.util.StringUtils;
import com.atlassian.core.util.collection.EasyList;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.tabpanels.GenericMessageAction;
import com.atlassian.jira.plugin.issuetabpanel.AbstractIssueTabPanel;
import com.atlassian.jira.plugins.bitbucket.webwork.BitbucketCommits;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.opensymphony.user.User;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.config.properties.PropertiesManager;

public class BitBucketTabPanel extends AbstractIssueTabPanel {

    final PluginSettingsFactory pluginSettingsFactory;
    final Logger logger = LoggerFactory.getLogger(BitBucketTabPanel.class);

    public String repositoryURL;
    public String repoLogin;
    public String repoName;
    public String branch;

    public BitBucketTabPanel(PluginSettingsFactory pluginSettingsFactory){
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

    protected void populateVelocityParams(Map params)
    {
        params.put("stringUtils", new StringUtils());
        params.put("github", this);
    }

    private String getRepositoryURLFromCommitURL(String commitURL){

        // https://api.bitbucket.org/1.0/repositories/mbuckbee/test-repository/changesets/8a4c58af4bd9?branch=default

        String[] arrayCommitURL = commitURL.split("/");
        String[] arrayBranch = commitURL.split("=");

        String branch = "";

        if(arrayBranch.length == 1){
            branch = "master";
        }else{
            branch = arrayBranch[1];
        }

        String repoBranchURL = "https://bitbucket.org/" + arrayCommitURL[5] + "/" + arrayCommitURL[6] + "/" + branch;
        logger.debug("RepoBranchURL: " + repoBranchURL);

        this.repositoryURL = repoBranchURL;
        this.repoLogin = arrayCommitURL[5];
        this.repoName = arrayCommitURL[6];
        this.branch = branch;

        return repoBranchURL;
    }

    public List getActions(Issue issue, User user) {
        String projectKey = issue.getProjectObject().getKey();
        String issueId = (String)issue.getKey();

        BitbucketCommits bitbucketCommits = new BitbucketCommits(pluginSettingsFactory);
        bitbucketCommits.projectKey = projectKey;

        ArrayList<String> commitArray = new ArrayList<String>();

        String issueCommitActions = "No Bitbucket changesets Found";

        ArrayList<Object> bitbucketActions = new ArrayList<Object>();

        // First Time Repository URL is saved
        if ((ArrayList<String>)pluginSettingsFactory.createSettingsForKey(projectKey).get("bitbucketIssueCommitArray" + issueId) != null){
            commitArray = (ArrayList<String>)pluginSettingsFactory.createSettingsForKey(projectKey).get("bitbucketIssueCommitArray" + issueId);

            for (int i=0; i < commitArray.size(); i++){
                    logger.debug("Found commit id " + commitArray.get(i));

                    bitbucketCommits.repositoryURL = getRepositoryURLFromCommitURL(commitArray.get(i));
                    String commitDetails = bitbucketCommits.getCommitDetails(commitArray.get(i));

                    issueCommitActions = this.formatCommitDetails(commitDetails);
                    GenericMessageAction action = new GenericMessageAction(issueCommitActions);
                    bitbucketActions.add(action);

                    logger.debug("Commit Entry: " + "bitbucketIssueCommitArray" + i );

            }

        }

        return EasyList.build(bitbucketActions);


    }

    public boolean showPanel(Issue issue, User user) {
        return true;  //To change body of implemented methods use File | Settings | File Templates.
    }

    private Date parseISO8601(String input) throws ParseException{
        //NOTE: SimpleDateFormat uses GMT[-+]hh:mm for the TZ which breaks
        //things a bit.  Before we go on we have to repair this.
        SimpleDateFormat df = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ssz" );

        //this is zero time so we need to add that TZ indicator for
        if ( input.endsWith( "Z" ) ) {
            input = input.substring( 0, input.length() - 1) + "GMT-00:00";
        } else {
            int inset = 6;

            String s0 = input.substring( 0, input.length() - inset );
            String s1 = input.substring( input.length() - inset, input.length() );

            input = s0 + "GMT" + s1;
        }

        return df.parse(input);

    }

    private String formatCommitDate(Date commitDate) throws ParseException{
        SimpleDateFormat sdfGithub = new SimpleDateFormat("MMM d yyyy KK:mm:ss");
        return sdfGithub.format(commitDate);
    }


    private String extractDiffInformation(String diff){

        if (!diff.trim().equals("")){
            // the +3 and -1 remove the leading and trailing spaces

            //logger.debug("Diff STring: " + diff);

            Integer first = diff.indexOf("@@") + 3;
            Integer second = diff.indexOf("@@", first) -1;

            //logger.debug("first: " + first.toString());
            //logger.debug("second: " + second.toString());

            String[] modLine = diff.substring(first,second).replace("+","").replace("-","").split(" ");

            String[] removedEntryArray = modLine[0].split(",");
            String[] addedEntryArray = modLine[1].split(",");

            String removedEntry = "";
            String addedEntry = "";

            if (removedEntryArray.length == 1){
                removedEntry = removedEntryArray[0];
            }else{
                removedEntry = removedEntryArray[1];
            }

            if (addedEntryArray.length == 1){
                addedEntry = addedEntryArray[0];
            }else{
                addedEntry = addedEntryArray[1];
            }

            if (addedEntry.trim().equals("0")){
                addedEntry = "<span style='color: gray'>+" + addedEntry + "</span>";
            }else{
                addedEntry = "<span style='color: green'>+" + addedEntry + "</span>";
            }

            if (removedEntry.trim().equals("0")){
                removedEntry = "<span style='color: gray'>-" + removedEntry + "</span>";
            }else{
                removedEntry = "<span style='color: red'>-" + removedEntry + "</span>";
            }


            return addedEntry + " " + removedEntry;
        }else{
            return "<span style='color: gray'>+0 -0</span>";
        }
    }

    private String fileCommitURL(String filename, String commitHash){
        String fileCommitURL = "https://bitbucket.org/" + repoLogin + "/" + repoName + "/src/" + commitHash + "/" + filename;
        return fileCommitURL;

    }

    private String formatCommitDetails(String jsonDetails){
            try{

                String baseURL = PropertiesManager.getInstance().getPropertySet().getString("jira.baseurl");


                JSONObject commit = new JSONObject(jsonDetails);

                String message = commit.getString("message");
                String commit_hash = commit.getString("node");

                String authorName = commit.getString("raw_author");
                String login = commit.getString("author");

                //https://bitbucket.org/mbuckbee/test-repository/changeset/8a4c58af4bd9

                String commitURL = "http://bitbucket.org/" + repoLogin + "/" + repoName + "/changeset/" + commit_hash;

                String projectName = repoName;

                String committedDateString = commit.getString("timestamp");

                String commitMessage = commit.getString("message");

                JSONObject bitbucketUser = new JSONObject(getUserDetails(login));
                JSONObject user = bitbucketUser.getJSONObject("user");

                //logger.debug(user.toString());

                String userName = user.getString("username");
                //String gravatarUrl = "https://secure.gravatar.com/avatar/6c07990e60f0d8e9c8e2c374a30b4639?d=identicon&s=60";
                String gravatarUrl = user.getString("avatar");
                gravatarUrl = gravatarUrl.replace("s=32","s=60");


                String htmlParentHashes = "";

                if(commit.has("parents")){
                    JSONArray arrayParents = commit.getJSONArray("parents");

                    for (int i=0; i < arrayParents.length(); i++){
                        String parentHashID = arrayParents.getString(i);
                        htmlParentHashes = "<tr><td style='color: #757575'>Parent:</td><td><a href='" + "https://bitbucket.org/" + login + "/" + projectName + "/changeset/" + parentHashID +"' target='_new'>" + parentHashID + "</a></td></tr>";

                    }

                }


                Map mapFiles = Collections.synchronizedMap(new TreeMap());

                String htmlFile = "";

                if(commit.has("files")){
                    JSONArray arrayAdded = commit.getJSONArray("files");

                    for (int i=0; i < arrayAdded.length(); i++){
                            JSONObject file = arrayAdded.getJSONObject(i);

                            String fileAction = file.getString("type");
                            String fileName = file.getString("file");

                            String color = "";
                            String fileActionName = "";

                            if (fileAction.equals("added")){
                                color = "green";
                                fileActionName = "ADDED";

                            }else if(fileAction.equals("removed")){
                                color = "red";
                                fileActionName = "REMOVED";

                            }else if(fileAction.equals("modified")){
                                color = "blue";
                                fileActionName = "MODIFIED";
                            }

                            htmlFile = "<li><span style='color:" + color + "; font-size: 8pt;'>" + fileActionName + "</span> <a href='" + fileCommitURL(fileName, commit_hash) + "' target='_new'>" + fileName + "</a></li>";
                            //logger.debug(htmlFile);
                            mapFiles.put(fileName, htmlFile);

                    }

                }

                String htmlFiles = "";
                String htmlFilesHiddenDescription = "";
                Integer numSeeMore = 0;
                Random randDivID = new Random(System.currentTimeMillis());

                // Sort and compose all files
                Iterator it = mapFiles.keySet().iterator();
                Object obj;

                String htmlHiddenDiv = "";

                if(mapFiles.size() <= 5){
                    while (it.hasNext()) {
                      obj = it.next();
                      htmlFiles += mapFiles.get(obj);
                    }

                    htmlFilesHiddenDescription = "";

                }else{

                    Integer i = 0;

                    while (it.hasNext()) {
                        obj = it.next();

                        if(i <= 4){
                            htmlFiles += mapFiles.get(obj);
                        }else{
                            htmlHiddenDiv += mapFiles.get(obj);
                        }

                        i++;
                    }

                    numSeeMore = mapFiles.size() - 5;
                    Integer divID = randDivID.nextInt();

                    htmlFilesHiddenDescription = "<div class='see_more' id='see_more_" + divID.toString() + "' style='color: #3C78B5; cursor: pointer; text-decoration: underline;' onclick='toggleMoreFiles(" + divID.toString() + ")'>" +
                                "See " + numSeeMore.toString() + " more" +
                            "</div>" +
                            "<div class='hide_more' id='hide_more_" + divID.toString() + "' style='display: none; color: #3C78B5;  cursor: pointer; text-decoration: underline;' onclick='toggleMoreFiles(" + divID.toString() + ")'>Hide " + numSeeMore.toString() + " Files</div>";

                    htmlHiddenDiv = htmlFilesHiddenDescription + "<div id='" + divID.toString() + "' style='display: none;'><ul>" + htmlHiddenDiv + "</ul></div>";

                }


String htmlCommitEntry = "" +
    "<table>" +
        "<tr>" +
            "<td valign='top' width='70px'><a href='#user_url' target='_new'><img src='#gravatar_url' border='0'></a></td>" +
            "<td valign='top'>" +
                "<div style='padding-bottom: 6px'><a href='#user_url' target='_new'>#user_name - #login</a></div>" +
                "<table>" +
                    "<tr>" +
                        "<td>" +
                            "<div style='border-left: 2px solid #C9D9EF; background-color: #EAF3FF; color: #5D5F62; padding: 5px; margin-bottom: 10px;'>#commit_message</div>" +

                                "<ul>" +
                                    htmlFiles +
                                "</ul>" +

                                htmlHiddenDiv +

                            "<div style='margin-top: 10px'>" +
                                "<img src='" + baseURL +"/download/resources/com.atlassian.jira.plugins.bitbucket.Bitbucket/images/document.jpg' align='center'> <span class='commit_date' style='color: #757575; font-size: 9pt;'>#formatted_commit_date</span>" +
                            "</div>" +

                        "</td>" +

                        "<td width='400' style='padding-top: 0px' valign='top'>" +
                            "<div style='border-left: 2px solid #cccccc; margin-left: 15px; margin-top: 0px; padding-top: 0px; padding-left: 10px'>" +
                                "<table style='margin-top: 0px; padding-top: 0px;'>" +
                                    "<tr><td style='color: #757575'>Changeset:</td><td><a href='#commit_url' target='_new'>#commit_hash</a></td></tr>" +
                                     htmlParentHashes +
                                "</table>" +
                            "</div>" +
                        "</td>" +

                    "</tr>" +
                "</table>" +
        "</td>" +
    "</tr>" +
"</table>";


                htmlCommitEntry = htmlCommitEntry.replace("#gravatar_url", gravatarUrl);
                htmlCommitEntry = htmlCommitEntry.replace("#user_url", "https://bitbucket.org/" + login);
                htmlCommitEntry = htmlCommitEntry.replace("#login", login);

                htmlCommitEntry = htmlCommitEntry.replace("#user_name", authorName);

                htmlCommitEntry = htmlCommitEntry.replace("#commit_message", commitMessage);

                htmlCommitEntry = htmlCommitEntry.replace("#formatted_commit_time", committedDateString);

                htmlCommitEntry = htmlCommitEntry.replace("#formatted_commit_date", committedDateString);

                htmlCommitEntry = htmlCommitEntry.replace("#commit_url", commitURL);
                htmlCommitEntry = htmlCommitEntry.replace("#commit_hash", commit_hash);

                //htmlCommitEntry = htmlCommitEntry.replace("#tree_url", "https://github.com/" + login + "/" + projectName + "/tree/" + commit_hash);

                //htmlCommitEntry = htmlCommitEntry.replace("#tree_hash", commitTree);





                return htmlCommitEntry;

             // Catches invalid or removed BitBucket IDs
            }catch (JSONException e){
                e.printStackTrace();
                return "";
            }

    }

    private String getUserDetails(String loginName){

        URL url;
        HttpURLConnection conn;

        BufferedReader rd;
        String line;
        String result = "";
        try {
            url = new URL("https://api.bitbucket.org/1.0/users/" + loginName);
            conn = (HttpURLConnection) url.openConnection();
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

        //logger.debug(result);

        return result;


    }


}
