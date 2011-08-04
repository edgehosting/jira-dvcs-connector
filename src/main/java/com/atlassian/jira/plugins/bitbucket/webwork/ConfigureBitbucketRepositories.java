package com.atlassian.jira.plugins.bitbucket.webwork;

import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.config.properties.PropertiesManager;
import com.atlassian.jira.plugins.bitbucket.property.BitbucketProjectSettings;
import com.atlassian.jira.plugins.bitbucket.property.BitbucketSyncProgress;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.net.RequestFactory;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Webwork action used to configure the bitbucket repositories
 */
public class ConfigureBitbucketRepositories extends JiraWebActionSupport
{
    private final Logger logger = LoggerFactory.getLogger(ConfigureBitbucketRepositories.class);
    private final BitbucketProjectSettings bitbucketProjectSettings;
    private final RequestFactory<?> requestFactory;

    // JIRA Project Listing
    private ComponentManager cm = ComponentManager.getInstance();
    private List<Project> projects = cm.getProjectManager().getProjectObjects();
    private String mode = "";
    private String bbUserName = "";
    private String bbPassword = "";
    private String url = "";
    private String postCommitURL = "";
    private String repoVisibility = "";
    private String projectKey = "";
    private String nextAction = "";
    private String validations = "";
    private String messages = "";
    private String redirectURL = "";

    public ConfigureBitbucketRepositories(BitbucketProjectSettings bitbucketProjectSettings,
                                          RequestFactory<?> requestFactory)
    {
        this.bitbucketProjectSettings = bitbucketProjectSettings;
        this.requestFactory = requestFactory;
    }

    protected void doValidation()
    {
        for (Enumeration e = request.getParameterNames(); e.hasMoreElements(); )
        {
            String n = (String) e.nextElement();
            String[] vals = request.getParameterValues(n);
            //validations = validations + "name " + n + ": " + vals[0];
        }

        // BitBucket URL Validation
        if (!url.equals(""))
        {
            if (nextAction.equals("AddRepository") || nextAction.equals("DeleteReposiory"))
            {
                // Valid URL and URL starts with bitbucket.org domain
                Pattern p = Pattern.compile("^(https|http)://bitbucket.org/[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
                Matcher m = p.matcher(url);
                if (!m.matches())
                {
                    addErrorMessage("URL must be for a valid Bitbucket.org repository.");
                    validations = "URL must be for a valid Bitbucket.org repository.";
                }
            }
        }
        else
        {
            if (nextAction.equals("AddRepository") || nextAction.equals("DeleteReposiory"))
            {
                validations = "URL must be for a valid Bitbucket.org repository.";
            }
        }

    }

    public String doDefault()
    {
        return "input";
    }

    @RequiresXsrfCheck
    protected String doExecute() throws Exception
    {
        logger.debug("configure repository [ " + nextAction + " ] [ " + requestFactory + " ]");

        // Remove trailing slashes from URL
        if (url.endsWith("/"))
        {
            url = url.substring(0, url.length() - 1);
        }

        // Set all URLs to HTTPS
        if (url.startsWith("http:"))
        {
            url = url.replaceFirst("http:", "https:");
        }

        // Swap overview with 'default' (likely to be pasted in)
        if (url.endsWith("/overview"))
        {
            url = url.substring(0, url.length() - 9);
            url = url + "/default";
        }

        // Add default branch of 'default' to URL if missing
        String[] urlArray = url.split("/");

        if (urlArray.length == 5)
        {
            url += "/default";
            urlArray = url.split("/");
        }

        if (validations.equals(""))
        {
            if (nextAction.equals("AddRepository"))
            {

                if (repoVisibility.equals("private"))
                {
                    if (StringUtils.isNotBlank(bbUserName) && StringUtils.isNotBlank(bbPassword))
                    {
                        Encryptor encryptor = new Encryptor();
                        String cipherText = encryptor.encrypt(bbPassword, projectKey, url);

                        // Store Username and Password for later Basic Auth
                        bitbucketProjectSettings.setUsername(projectKey, url, bbUserName);
                        bitbucketProjectSettings.setPassword(projectKey, url, cipherText);

                        postCommitURL = "BitbucketPostCommit.jspa?projectKey=" + projectKey + "&branch=" + urlArray[urlArray.length - 1];
                        addRepositoryURL();
                        nextAction = "ForceSync";
                    }
                }
                else
                {
                    postCommitURL = "BitbucketPostCommit.jspa?projectKey=" + projectKey + "&branch=" + urlArray[urlArray.length - 1];
                    logger.debug(postCommitURL);
                    addRepositoryURL();
                    nextAction = "ForceSync";
                }

            }

            if (nextAction.equals("ShowPostCommitURL"))
            {
                postCommitURL = "BitbucketPostCommit.jspa?projectKey=" + projectKey + "&branch=" + urlArray[urlArray.length - 1];
            }

            if (nextAction.equals("DeleteRepository"))
            {
                deleteRepositoryURL();
            }

            if (nextAction.equals("CurrentSyncStatus"))
            {
                return "syncstatus";
            }

            if (nextAction.equals("SyncRepository"))
            {
                syncRepository();
                return "syncmessage";
            }
        }

        return INPUT;
    }

    private void resetCommitTotals()
    {
        bitbucketProjectSettings.startSyncProgress(projectKey, url);
        bitbucketProjectSettings.resetCount(projectKey, url, BitbucketCommits.COUNT_JIRA, 0);
        bitbucketProjectSettings.resetCount(projectKey, url, BitbucketCommits.COUNT_NON_JIRA, 0);
    }

    // Manages the entry of multiple repository URLs in a single pluginSetting Key
    private void addRepositoryURL()
    {
        logger.debug("add repository [ {} ] to [ {} ]", url, projectKey);
        List<String> repositories = bitbucketProjectSettings.getRepositories(projectKey);
        if (!repositories.contains(url))
        {
            repositories.add(url);
            resetCommitTotals();
            bitbucketProjectSettings.setRepositories(projectKey, repositories);
        }
    }


    private void syncRepository()
    {
        logger.debug("sync [ {} ] for project [ {} ]", url, projectKey);

        BitbucketCommits repositoryCommits = new BitbucketCommits(bitbucketProjectSettings);
        repositoryCommits.repositoryURL = url;
        repositoryCommits.projectKey = projectKey;

        // Reset Commit count
        resetCommitTotals();

        // Starts actual search of commits via Bitbucket API, "0" designates the 'start' parameter
        messages = repositoryCommits.syncAllCommits();
    }

    // Removes a single Repository URL from a given Project
    private void deleteRepositoryURL()
    {
        List<String> repositories = bitbucketProjectSettings.getRepositories(projectKey);
        if (repositories.contains(url))
        {
            BitbucketCommits repositoryCommits = new BitbucketCommits(bitbucketProjectSettings);
            repositoryCommits.repositoryURL = url;
            repositoryCommits.projectKey = projectKey;

            repositoryCommits.removeRepositoryIssueIDs();
            repositories.remove(url);
            bitbucketProjectSettings.setRepositories(projectKey, repositories);
        }
    }

    // Used to provide URLs on the repository management screen that go to actual pages
    // as the service does not support repo urls with branches
    public String getRepositoryURLWithoutBranch(String repoURL)
    {
        Integer lastSlash = repoURL.lastIndexOf("/");
        return repoURL.substring(0, lastSlash);
    }

    public List getProjects()
    {
        return projects;
    }

    // Stored Repository + JIRA Projects
    public List<String> getProjectRepositories(String projectKey)
    {
        return bitbucketProjectSettings.getRepositories(projectKey);
    }

    public String getProjectName()
    {
        return cm.getProjectManager().getProjectObjByKey(projectKey).getName();
    }

    public void setMode(String value)
    {
        this.mode = value;
    }

    public String getMode()
    {
        return mode;
    }

    public void setbbUserName(String value)
    {
        this.bbUserName = value;
    }

    public String getbbUserName()
    {
        return this.bbUserName;
    }

    public void setbbPassword(String value)
    {
        this.bbPassword = value;
    }

    public String getbbPassword()
    {
        return this.bbPassword;
    }

    public void setUrl(String value)
    {
        this.url = value;
    }

    public String getURL()
    {
        return url;
    }

    public void setPostCommitURL(String value)
    {
        this.postCommitURL = value;
    }

    public String getPostCommitURL()
    {
        return postCommitURL;
    }

    public void setRepoVisibility(String value)
    {
        this.repoVisibility = value;
    }

    public String getRepoVisibility()
    {
        return repoVisibility;
    }

    public void setProjectKey(String value)
    {
        this.projectKey = value;
    }

    public String getProjectKey()
    {
        return projectKey;
    }

    public void setNextAction(String value)
    {
        this.nextAction = value;
    }

    public String getNextAction()
    {
        return this.nextAction;
    }

    public String getValidations()
    {
        return this.validations;
    }

    public String getMessages()
    {
        return this.messages;
    }

    public String getRedirectURL()
    {
        return this.redirectURL;
    }

    public int getNonJIRACommitTotal()
    {
        return this.bitbucketProjectSettings.getCount(projectKey, url, BitbucketCommits.COUNT_NON_JIRA);
    }

    public int getJIRACommitTotal()
    {
        return this.bitbucketProjectSettings.getCount(projectKey, url, BitbucketCommits.COUNT_JIRA);
    }

    public BitbucketSyncProgress getSyncProgress()
    {
        return bitbucketProjectSettings.getSyncProgress(projectKey, url);
    }
}
