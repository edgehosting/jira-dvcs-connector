package com.atlassian.jira.plugins.bitbucket.webwork;

import com.atlassian.jira.config.CoreFeatures;
import com.atlassian.jira.config.FeatureManager;
import com.atlassian.jira.plugins.bitbucket.api.RepositoryManager;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.api.exception.SourceControlException;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.ApplicationProperties;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.List;

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
    private String addedRepositoryId = "";
    private int repositoryId;
    private final String baseUrl;

    private final RepositoryManager globalRepositoryManager;
    private String postCommitRepositoryType;
    private FeatureManager featureManager;

    public ConfigureBitbucketRepositories(
            @Qualifier("globalRepositoryManager") RepositoryManager globalRepositoryManager,
            ApplicationProperties applicationProperties, FeatureManager featureManager)
    {
        this.globalRepositoryManager = globalRepositoryManager;
        this.baseUrl = applicationProperties.getBaseUrl();
        this.featureManager = featureManager;
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
                    try
                    {
                        SourceControlRepository repo = globalRepositoryManager.getRepository(repositoryId);
                        postCommitUrl = baseUrl + "/rest/bitbucket/1.0/repository/" + repositoryId + "/sync";
                        postCommitRepositoryType = StringUtils.capitalize(repo.getRepositoryType());
                    } catch (Exception e)
                    {
                        // do nothing. repository not found. it may be deleted

                        // TODO instead of catching exception we should make sure this action is not
                        // called on deleted repositories BBC-48
                    }
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

    public String getPostCommitRepositoryType()
    {
        return postCommitRepositoryType;
    }

    public void setPostCommitRepositoryType(String postCommitRepositoryType)
    {
        this.postCommitRepositoryType = postCommitRepositoryType;
    }

    public boolean isOnDemandLicense()
    {
        return featureManager.isEnabled(CoreFeatures.ON_DEMAND);
    }
}
