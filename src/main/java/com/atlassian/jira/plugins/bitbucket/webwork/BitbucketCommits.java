package com.atlassian.jira.plugins.bitbucket.webwork;

import com.atlassian.jira.plugins.bitbucket.property.BitbucketProjectSettings;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.BASE64Encoder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BitbucketCommits
{
    public static final String COUNT_JIRA = "JIRACommitTotal";
    public static final String COUNT_NON_JIRA = "NonJIRACommitTotal";
    public static final int PAGE_SIZE = 50;

    public String repositoryURL;
    public String projectKey;

    private final Logger logger = LoggerFactory.getLogger(BitbucketCommits.class);
    private final BitbucketProjectSettings bitbucketProjectSettings;

    public BitbucketCommits(BitbucketProjectSettings bitbucketProjectSettings)
    {
        this.bitbucketProjectSettings = bitbucketProjectSettings;
    }

    // Generates a URL for pulling commit messages based upon the base Repository URL
    // https://bitbucket.org/ellislab/codething/master
    private String inferCommitsURL()
    {
        logger.debug("BitBucketCommits.inferCommitsURL() - " + repositoryURL);
        String[] path = repositoryURL.split("/");
        return "https://api.bitbucket.org/1.0/repositories/" + path[3] + "/" + path[4] + "/changesets";
    }

    // Generate a URL for pulling a single commits details (diff and author)
    private String inferCommitDetailsURL()
    {
        String[] path = repositoryURL.split("/");
        return "https://api.bitbucket.org/1.0/repositories/" + path[3] + "/" + path[4] + "/changesets/";
    }

    private String getBranchFromURL()
    {
        String[] path = repositoryURL.split("/");
        return path[5];
    }

    private String getCommitsList(Integer startNumber)
    {
        logger.debug("BitbucketCommits.getCommitsList(" + startNumber + ")");
        URL url;
        HttpURLConnection conn;

        BufferedReader rd;
        String line;
        String result = "";

        try
        {
            if (startNumber == null)
                url = new URL(this.inferCommitsURL() + "?limit=" + PAGE_SIZE);
            else
                url = new URL(this.inferCommitsURL() + "?start=" + startNumber + "&limit=" + PAGE_SIZE);

            logger.debug("URL: " + url);
            logger.debug("BitbucketCommits.getCommitsList() - Commits URL - " + url);

            conn = (HttpURLConnection) url.openConnection();

            addAuthorizationTokenToConnection(conn);

            conn.setInstanceFollowRedirects(true);
            conn.setRequestMethod("GET");
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while ((line = rd.readLine()) != null)
            {
                result += line;
            }
            rd.close();

            // Sets current page status for UI feedback
            if (startNumber == null)
                bitbucketProjectSettings.startSyncProgress(projectKey, repositoryURL);
            else
                bitbucketProjectSettings.setSyncProgress(projectKey, repositoryURL, startNumber);

        }
        catch (MalformedURLException e)
        {
            logger.error("BitbucketCommits.getCommitsList() - Malformed exception", e);
            if (startNumber != null && startNumber == 0)
                result = "Bitbucket Repository can't be found or incorrect credentials.";

            bitbucketProjectSettings.completeSyncProgress(projectKey, repositoryURL);
        }
        catch (Exception e)
        {
            logger.error("BitbucketCommits.getCommitsList() - End of Commits or Unauthorized", e);
            if (startNumber != null && startNumber == 0)
                result = "Bitbucket Repository can't be found or incorrect credentials.";

            bitbucketProjectSettings.completeSyncProgress(projectKey, repositoryURL);
        }

        return result;
    }

    // Commit list returns id (hashed) and Message
    // you have to call each individual commit to get diff details
    public String getCommitDetails(String commit_id_url)
    {
        URL url;
        HttpURLConnection conn;

        BufferedReader rd;
        String line;
        String result = "";
        try
        {
            url = new URL(commit_id_url);
            conn = (HttpURLConnection) url.openConnection();

            logger.debug("BitbucketCommits().getCommitDetails()");
            addAuthorizationTokenToConnection(conn);

            conn.setInstanceFollowRedirects(true);
            conn.setRequestMethod("GET");
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while ((line = rd.readLine()) != null)
            {
                result += line;
            }
            rd.close();
        }
        catch (Exception e)
        {
            logger.error("error loading commit details", e);
        }

        return result;
    }

    private void addAuthorizationTokenToConnection(URLConnection connection)
    {
        String bbUserName = bitbucketProjectSettings.getUsername(projectKey, repositoryURL);
        String bbPassword = bitbucketProjectSettings.getPassword(projectKey, repositoryURL);

        if (StringUtils.isNotEmpty(bbUserName) && StringUtils.isNotEmpty(bbPassword))
        {
            logger.debug("BitbucketCommits() - Using Basic Auth");
            logger.debug("URL: " + repositoryURL);
            logger.debug("Username: " + bbUserName);

            Encryptor encryptor = new Encryptor();
            bbPassword = encryptor.decrypt(bbPassword, projectKey, repositoryURL);

            BASE64Encoder enc = new sun.misc.BASE64Encoder();
            String userpassword = bbUserName + ":" + bbPassword;
            String encodedAuthorization = enc.encode(userpassword.getBytes());

            connection.setRequestProperty("Authorization", "Basic " + encodedAuthorization);
        }
    }

    private Set<String> extractProjectKey(String message)
    {
        Pattern projectKeyPattern = Pattern.compile("(" + this.projectKey + "-\\d*)", Pattern.CASE_INSENSITIVE);
        Matcher match = projectKeyPattern.matcher(message);

        Set<String> matches = new HashSet<String>();

        while (match.find())
        {
            // Get all groups for this match
            for (int i = 0; i <= match.groupCount(); i++)
            {
                matches.add(match.group(i));
            }
        }

        return matches;
    }

    public String syncAllCommits()
    {
        try
        {
            return syncCommits(null); // sync all commits starting from the tip
        }
        finally
        {
            bitbucketProjectSettings.completeSyncProgress(projectKey, repositoryURL);
        }
    }

    public String syncCommits(Integer startNumber)
    {

        if (startNumber != null && startNumber <= 0)
            return "";

        logger.debug("BitbucketCommits.syncCommits() - startNumber: " + (startNumber == null ? "tip" : startNumber));
        String commitsAsJSON = getCommitsList(startNumber);
        String messages = "";

        if (commitsAsJSON != "")
        {

            try
            {
                JSONObject jsonCommits = new JSONObject(commitsAsJSON);
                JSONArray commits = jsonCommits.getJSONArray("changesets");

                int lowestRevision = Integer.MAX_VALUE;

                for (int i = 0; i < commits.length(); ++i)
                {
                    String message = commits.getJSONObject(i).getString("message");
                    String commit_id = commits.getJSONObject(i).getString("node");
                    lowestRevision = Math.min(lowestRevision, commits.getJSONObject(i).getInt("revision"));

                    // Detect presence of JIRA Issue Key
                    if (message.indexOf(this.projectKey) > -1)
                    {
                        Set<String> extractedIssues = extractProjectKey(message);
                        for (String extractedIssue : extractedIssues)
                        {
                            String issueId = extractedIssue.toString().toUpperCase();
                            addCommitID(issueId, commit_id, getBranchFromURL());
                            bitbucketProjectSettings.incrementCommitCount(projectKey, repositoryURL, COUNT_JIRA);
                        }
                    }
                    else
                    {
                        bitbucketProjectSettings.incrementCommitCount(projectKey, repositoryURL, COUNT_NON_JIRA);
                    }

                }
                return messages += this.syncCommits(lowestRevision - 1);

            }
            catch (JSONException e)
            {
                logger.error("BitbucketCommits.syncCommits() - Exception", e);
                //e.printStackTrace();
                return "Bitbucket repository can't be found or incorrect credentials.";
            }
        }

        return messages;

    }

    public String postReceiveHook(String payload)
    {
        logger.debug("BitbuckbetCommits.postReceiveHook()");
        String messages = "";

        try
        {
            JSONObject jsonCommits = new JSONObject(payload);
            JSONArray commits = jsonCommits.getJSONArray("commits");

            for (int i = 0; i < commits.length(); ++i)
            {
                String message = commits.getJSONObject(i).getString("message").toLowerCase();
                String commit_id = commits.getJSONObject(i).getString("node");

                // Detect presence of JIRA Issue Key
                if (message.indexOf(this.projectKey.toLowerCase()) > -1)
                {
                    Set<String> extractedIssues = extractProjectKey(message);
                    for (String extractedIssue : extractedIssues)
                    {
                        String issueId = extractedIssue.toString().toUpperCase();
                        addCommitID(issueId, commit_id, getBranchFromURL());
                        bitbucketProjectSettings.incrementCommitCount(projectKey, repositoryURL, COUNT_JIRA);
                    }

                }
                else
                {
                    bitbucketProjectSettings.incrementCommitCount(projectKey, repositoryURL, COUNT_NON_JIRA);
                }
            }


        }
        catch (JSONException e)
        {
            //e.printStackTrace();
            return "exception";
        }

        return messages;
    }


    private String getRepositoryURLFromCommitURL(String commitURL)
    {

        // Changeset URL example
        // https://api.bitbucket.org/1.0/repositories/jespern/django-piston/changesets/fa57572a9acf?branch=master

        String[] arrayCommitURL = commitURL.split("/");
        String[] arrayBranch = commitURL.split("=");

        String branch = "";

        if (arrayBranch.length == 1)
        {
            branch = "default";
        }
        else
        {
            branch = arrayBranch[1];
        }

        String repoBranchURL = "https://bitbucket.org/" + arrayCommitURL[5] + "/" + arrayCommitURL[6] + "/" + branch;
        logger.debug("bitbucketCommits.getRepositoryURLFromCommitURL() - RepoBranchURL: " + repoBranchURL);
        return repoBranchURL;
    }

    // Manages the entry of multiple Bitbucket commit id hash ids associated with an issue
    // urls look like - https://api.bitbucket.org/1.0/repositories/jespern/django-piston/changesets/fa57572a9acf?branch=master
    private void addCommitID(String issueId, String commitId, String branch)
    {
        List<String> commits = bitbucketProjectSettings.getCommits(projectKey, repositoryURL, issueId);

        String commitUrl = inferCommitDetailsURL() + commitId + "?branch=" + branch;
        if (!commits.contains(commitUrl))
        {
            logger.debug("addCommitID: Adding CommitID " + inferCommitDetailsURL() + commitId);
            commits.add(commitUrl);
            addIssueId(issueId);
            bitbucketProjectSettings.setCommits(projectKey, repositoryURL, issueId, commits);
        }
    }

    // Removes a specific commit_id (URL) from the saved array
    private void removeCommitID(String issueId, String commitUrl)
    {
        logger.debug("remove commit [ " + commitUrl + " ] from [ " + issueId + " ]");
        List<String> commits = bitbucketProjectSettings.getCommits(projectKey, repositoryURL, issueId);
        if (!commits.remove(commitUrl))
            bitbucketProjectSettings.setCommits(projectKey, repositoryURL, issueId, commits);
    }

    // Manages the recording of items ids for a JIRA project + Repository Pair so that we know
    // which issues within a project have commits associated with them
    private void addIssueId(String issueId)
    {
        List<String> ids = bitbucketProjectSettings.getIssueIds(projectKey, repositoryURL);
        if (!ids.contains(issueId))
        {
            logger.debug("Bitbucket.addIssueId() - " + issueId);
            ids.add(issueId);
            bitbucketProjectSettings.setIssueIds(projectKey, repositoryURL, ids);
        }
    }

    // Removes all record of issues associated with this project and repository URL
    public void removeRepositoryIssueIDs()
    {
        List<String> ids = bitbucketProjectSettings.getIssueIds(projectKey, repositoryURL);
        // Array of JIRA Issue IDS like ['PONE-4','PONE-10']
        for (String id : ids)
        {
            List<String> commits = bitbucketProjectSettings.getCommits(projectKey, repositoryURL, id);
            // Array of Commit URL IDs like ['http://bitbucket.org/...']
            for (String commit : commits)
            {
                logger.debug("BitbucketCommits.removeRepositoryIssueIDs() - Commit ID: " + commit);
                logger.debug("BitbucketCommits.removeRepositoryIssueIDs() - " + getRepositoryURLFromCommitURL(commit));
                if (repositoryURL.equals(getRepositoryURLFromCommitURL(commit)))
                    removeCommitID(id, commit);
            }
        }
    }

}
