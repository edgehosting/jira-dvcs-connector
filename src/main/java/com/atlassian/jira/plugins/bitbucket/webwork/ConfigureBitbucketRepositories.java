package com.atlassian.jira.plugins.bitbucket.webwork;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import com.atlassian.jira.plugins.bitbucket.api.SourceControlException;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryManager;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.ApplicationProperties;

/**
 * Webwork action used to configure the bitbucket repositories
 * TODO test this on project page (mode='single')
 */
public class ConfigureBitbucketRepositories extends JiraWebActionSupport
{
    private final Logger logger = LoggerFactory.getLogger(ConfigureBitbucketRepositories.class);

    private String mode = "";
    private String repositoryUrl = "";
    private String postCommitUrl = "";
    private String projectKey = "";
    private String nextAction = "";
    private String addedRepositoryId="";
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
    }

    @Override
    @RequiresXsrfCheck
    protected String doExecute() throws Exception
    {
        logger.debug("configure repository [ " + nextAction + " ]");

        try
        {
            if (getErrorMessages().isEmpty())
            {
                if (nextAction.equals("ShowPostCommitURL"))
                {
                    postCommitUrl = baseUrl + "/rest/bitbucket/1.0/repository/" + repositoryId + "/sync";
                }

                if (nextAction.equals("DeleteRepository"))
                {
                    SourceControlRepository repo = globalRepositoryManager.getRepository(repositoryId);
                    globalRepositoryManager.removeRepository(repositoryId);
                    globalRepositoryManager.removePostcommitHook(repo);
                }
            }
        } catch (SourceControlException e)
        {
            addErrorMessage(e.getMessage());
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
    public void setRepositoryUrl(String value)
    {
        this.repositoryUrl = value;
    }

    public String getRepositoryUrl()
    {
        return repositoryUrl;
    }

    public void setPostCommitUrl(String value)
    {
        this.postCommitUrl = value;
    }

    public String getPostCommitUrl()
    {
        return postCommitUrl;
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

    public int getRepositoryId()
    {
        return repositoryId;
    }

    public void setRepositoryId(int repositoryId)
    {
        this.repositoryId = repositoryId;
    }

    public String getAddedRepositoryId()
    {
        return addedRepositoryId;
    }

    public void setAddedRepositoryId(String addedRepositoryId)
    {
        this.addedRepositoryId = addedRepositoryId;
    }

}
