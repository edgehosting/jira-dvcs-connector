package com.atlassian.jira.plugins.bitbucket.webwork;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryManager;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.ApplicationProperties;

/**
 * Webwork action used to configure the bitbucket repositories
 */
public class ConfigureBitbucketRepositories extends JiraWebActionSupport
{
    private final Logger logger = LoggerFactory.getLogger(ConfigureBitbucketRepositories.class);

    private String mode = "";
    private String adminUsername = "";
    private String adminPassword = "";
    private String bbUserName = "";
    private String bbPassword = "";
    private String url = "";
    private String postCommitURL = "";
    private String repoVisibility = "";
    private String projectKey = "";
    private String nextAction = "";
    private String validations = "";
    private final String redirectURL = "";
    private String addPostCommitService = "";
    private int repositoryId;
    private final String baseUrl;

    private final RepositoryManager globalRepositoryManager;

    public ConfigureBitbucketRepositories(
            @Qualifier("globalRepositoryManager") RepositoryManager globalRepositoryManager,
            ApplicationProperties applicationProperties)
    {
        this.globalRepositoryManager = globalRepositoryManager;
        this.baseUrl = applicationProperties.getBaseUrl();
    }

    @Override
    protected void doValidation()
    {
        if (!globalRepositoryManager.canHandleUrl(url) && nextAction.equals("AddRepository"))
        {
            addErrorMessage("URL must be for a valid repository.");
            validations = "URL must be for a valid repository.";
        }
    }

    @Override
    public String doDefault()
    {
        return "input";
    }

    @Override
    @RequiresXsrfCheck
    protected String doExecute() throws Exception
    {
        logger.debug("configure repository [ " + nextAction + " ]");

        if (validations.equals(""))
        {
            if (nextAction.equals("AddRepository"))
            {
                if (!repoVisibility.equals("private")
                        || (StringUtils.isNotBlank(bbUserName) && StringUtils.isNotBlank(bbPassword)))
                {
                    SourceControlRepository repo = globalRepositoryManager.addRepository(projectKey, url, bbUserName,
                            bbPassword, adminUsername, adminPassword);
                    if (BooleanUtils.toBoolean(addPostCommitService))
                    {
                        globalRepositoryManager.setupPostcommitHook(repo);
                    }
                    repositoryId = repo.getId();
                    postCommitURL = baseUrl + "/rest/bitbucket/1.0/repository/" + repositoryId + "/sync";
                    nextAction = "ForceSync";
                }
            }

            if (nextAction.equals("ShowPostCommitURL"))
            {
                postCommitURL = "BitbucketPostCommit.jspa?projectKey=" + projectKey + "&repositoryUrl="
                        + encodeUrl(url);
            }

            if (nextAction.equals("DeleteRepository"))
            {
                SourceControlRepository repo = globalRepositoryManager.getRepository(repositoryId);
                globalRepositoryManager.removeRepository(repositoryId);
                globalRepositoryManager.removePostcommitHook(repo);
            }
        }

        return INPUT;
    }

    public List<Project> getProjects()
    {
        return getProjectManager().getProjectObjects();
    }

    // Stored Repository + JIRA Projects
    public List<SourceControlRepository> getProjectRepositories(String projectKey)
    {
        return globalRepositoryManager.getRepositories(projectKey);
    }

    public String getProjectName()
    {
        return getProjectManager().getProjectObjByKey(projectKey).getName();
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

    public String getRedirectURL()
    {
        return this.redirectURL;
    }

    public int getRepositoryId()
    {
        return repositoryId;
    }

    public void setRepositoryId(int repositoryId)
    {
        this.repositoryId = repositoryId;
    }

    public String getAdminUsername()
    {
        return adminUsername;
    }

    public void setAdminUsername(String serviceUsername)
    {
        this.adminUsername = serviceUsername;
    }

    public String getAdminPassword()
    {
        return adminPassword;
    }

    public void setAdminPassword(String servicePassword)
    {
        this.adminPassword = servicePassword;
    }

    public String getAddPostCommitService()
    {
        return addPostCommitService;
    }

    public void setAddPostCommitService(String addPostCommitService)
    {
        this.addPostCommitService = addPostCommitService;
    }

    public static String encodeUrl(String url)
    {
        try
        {
            return URLEncoder.encode(url, "UTF-8");
        } catch (UnsupportedEncodingException e)
        {
            return null;
        }
    }
}
