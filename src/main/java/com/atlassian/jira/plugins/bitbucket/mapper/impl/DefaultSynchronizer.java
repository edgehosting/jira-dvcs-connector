package com.atlassian.jira.plugins.bitbucket.mapper.impl;

import com.atlassian.jira.plugins.bitbucket.bitbucket.*;
import com.atlassian.jira.plugins.bitbucket.mapper.BitbucketMapper;
import com.atlassian.jira.plugins.bitbucket.mapper.Synchronizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Synchronization services
 */
public class DefaultSynchronizer implements Synchronizer
{
    private final Logger logger = LoggerFactory.getLogger(DefaultSynchronizer.class);
    private final Bitbucket bitbucket;
    private final BitbucketMapper bitbucketMapper;

    public DefaultSynchronizer(Bitbucket bitbucket, BitbucketMapper bitbucketMapper)
    {
        this.bitbucket = bitbucket;
        this.bitbucketMapper = bitbucketMapper;
    }

    private Set<String> extractProjectKey(String projectKey, String message)
    {
        Pattern projectKeyPattern = Pattern.compile("(" + projectKey + "-\\d*)", Pattern.CASE_INSENSITIVE);
        Matcher match = projectKeyPattern.matcher(message);

        Set<String> matches = new HashSet<String>();

        while (match.find())
        {
            // Get all groups for this match
            for (int i = 0; i <= match.groupCount(); i++)
                matches.add(match.group(i));
        }

        return matches;
    }

    public void synchronize(String projectKey, String owner, String slug)
    {
        logger.debug("synchronize [ {} ] [ {} ] with [ {} ]",
                new Object[]{owner, slug, projectKey});

        BitbucketAuthentication auth = bitbucketMapper.getAuthentication(projectKey, owner, slug);
        List<BitbucketChangeset> changesets = bitbucket.getChangesets(auth, owner, slug);
        for (BitbucketChangeset changeset : changesets)
        {
            String message = changeset.getMessage();

            if (message.contains(projectKey))
            {
                Set<String> extractedIssues = extractProjectKey(projectKey, message);
                for (String extractedIssue : extractedIssues)
                {
                    String issueId = extractedIssue.toUpperCase();
                    bitbucketMapper.addChangeset(issueId, changeset);
                }
            }
        }
    }

    public void postReceiveHook(String payload)
    {
//        String messages = "";
//
//        try
//        {
//            JSONObject jsonCommits = new JSONObject(payload);
//            JSONArray commits = jsonCommits.getJSONArray("commits");
//
//            for (int i = 0; i < commits.length(); ++i)
//            {
//                String message = commits.getJSONObject(i).getString("message").toLowerCase();
//                String commit_id = commits.getJSONObject(i).getString("node");
//
//                // Detect presence of JIRA Issue Key
//                if (message.indexOf(this.projectKey.toLowerCase()) > -1)
//                {
//                    Set<String> extractedIssues = extractProjectKey(message);
//                    for (String extractedIssue : extractedIssues)
//                    {
//                        String issueId = extractedIssue.toString().toUpperCase();
//                        addCommitID(issueId, commit_id, getBranchFromURL());
//                        bitbucketProjectSettings.incrementCommitCount(projectKey, repositoryURL, COUNT_JIRA);
//                    }
//
//                }
//                else
//                {
//                    bitbucketProjectSettings.incrementCommitCount(projectKey, repositoryURL, COUNT_NON_JIRA);
//                }
//            }
//
//
//        }
//        catch (JSONException e)
//        {
//            logger.error("could not parse json payload from bitbucket post commit hook", e);
//        }
//
//        return messages;
    }

}
