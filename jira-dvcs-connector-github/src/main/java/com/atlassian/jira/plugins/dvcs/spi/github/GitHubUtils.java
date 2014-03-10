package com.atlassian.jira.plugins.dvcs.spi.github;

import org.eclipse.egit.github.core.client.IGitHubConstants;

/**
 * Provides utilities related to the GitHub.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubUtils
{

    /**
     * Only static member.
     */
    private GitHubUtils()
    {
    }

    /**
     * @param repoOwner
     *            name of the repository owner
     * @param repoName
     *            name of the repository
     * @param sha
     *            identity
     * @return commit HTML URL
     */
    public static String getHtmlUrlCommit(String repoOwner, String repoName, String sha)
    {
        return IGitHubConstants.PROTOCOL_HTTPS + "://" + IGitHubConstants.HOST_DEFAULT + "/" + repoOwner + "/" + repoName + "/commit/"
                + sha;
    }

}
