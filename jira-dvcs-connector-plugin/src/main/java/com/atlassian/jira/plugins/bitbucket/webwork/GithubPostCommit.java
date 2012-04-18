package com.atlassian.jira.plugins.bitbucket.webwork;

import com.atlassian.jira.plugins.bitbucket.Synchronizer;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.rest.RootResource;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryManager;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.List;

/**
 * Webwork action used to recieve the callback hook from github
 * Deprecated, use {@link RootResource} instead 
 */
@Deprecated 
public class GithubPostCommit extends JiraWebActionSupport
{
    private final Logger log = LoggerFactory.getLogger(GithubPostCommit.class);

    private String validations = "";
    private String projectKey = "";
    private String payload = "";

    private final Synchronizer synchronizer;
	private final RepositoryManager globalRepositoryManager;

    public GithubPostCommit(@Qualifier("globalRepositoryManager") RepositoryManager globalRepositoryManager, 
    		Synchronizer synchronizer)
    {
        this.globalRepositoryManager = globalRepositoryManager;
		this.synchronizer = synchronizer;
    }

    @Override
    protected void doValidation()
    {
        if (StringUtils.isBlank(projectKey))
        {
            validations += "Missing required parameter 'projectKey'<br/>";
        }

        if (StringUtils.isBlank(payload))
        {
            validations += "Missing required 'payload' parameter. <br/>";
        }
    }

    @Override
    protected String doExecute() throws Exception
    {
        if (StringUtils.isBlank(validations))
        {
            log.debug("Received call to sync payload ["+payload+"] with projectKey ["+projectKey+"]");
            JSONObject jsonPayload = new JSONObject(payload);
            JSONObject jsonRepository = jsonPayload.getJSONObject("repository");

            String baseRepositoryURL = jsonRepository.getString("url");

            List<SourceControlRepository> repositories = globalRepositoryManager.getRepositories(projectKey);
            for (SourceControlRepository repo : repositories)
            {
                if (repo.getRepositoryUri().getRepositoryUrl().equals(baseRepositoryURL))
                {
                    synchronizer.synchronize(repo, true);
                }
            }
        }        
        return "postcommit";
    }

	public String getValidations()
    {
        return this.validations;
    }

    public void setProjectKey(String value)
    {
        this.projectKey = value;
    }

    public String getProjectKey()
    {
        return projectKey;
    }

    public void setPayload(String value)
    {
        this.payload = value;
    }

    public String getPayload()
    {
        return payload;
    }

}
