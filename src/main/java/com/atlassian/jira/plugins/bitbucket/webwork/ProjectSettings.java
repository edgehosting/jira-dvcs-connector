package com.atlassian.jira.plugins.bitbucket.webwork;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Qualifier;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.plugin.projectoperation.AbstractPluggableProjectOperation;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryManager;
import com.atlassian.jira.project.Project;
import com.atlassian.sal.api.ApplicationProperties;

public class ProjectSettings extends AbstractPluggableProjectOperation
{
    private static final Pattern BITBUCKET_NAME_PATTERN = Pattern.compile(".*bitbucket.org/([^/]+/[^/]+)(/default)?");
	private final RepositoryManager globalRepositoryManager;
	private final ApplicationProperties applicationProperties;

    public ProjectSettings(@Qualifier("globalRepositoryManager") RepositoryManager repositoryManager, ApplicationProperties applicationProperties)
    {
        this.globalRepositoryManager = repositoryManager;
		this.applicationProperties = applicationProperties;
    }

    @Override
    public String getHtml(final Project project, final User user)
    {

        String baseURL = applicationProperties.getBaseUrl();

        List<SourceControlRepository> repositories = globalRepositoryManager.getRepositories(project.getKey());
        StringBuilder result = new StringBuilder();
        result.append("<span class=\"project-config-list-label\">");
        if (repositories.size() > 1)
        {
            result.append("Bitbucket Repositories:");
        }
        else
        {
            result.append("Bitbucket Repository:");
        }
        result.append("</span>\n")
                .append("<span class=\"project-config-list-value\">");

        switch (repositories.size())
        {
            case 0:
                result.append("None");
                break;
            case 1:
                result.append(getRepositoryName(repositories.get(0).getRepositoryUri().getRepositoryUrl()));
                break;
            default:
                result.append(repositories.size()).append(" repositories");
        }
        result.append(" (<a href='")
                .append(baseURL)
                .append("/secure/admin/ConfigureBitbucketRepositories!default.jspa?projectKey=")
                .append(project.getKey())
                .append("&mode=single'>")
                .append("Configure</a>)");
        return result.toString();

    }

    /**
     * Tries to extract repository name from URL
     * @param repoUrl The repo url, shouldn't be null, but could be
     * @return The text to tell the user about this repo
     */
    String getRepositoryName(String repoUrl)
    {
        String result = "One repository";
        if (repoUrl != null)
        {
            Matcher matcher = BITBUCKET_NAME_PATTERN.matcher(repoUrl);
            if (matcher.matches())
            {
                result = matcher.group(1);
            }
        }
        return result;
    }

    @Override
    public boolean showOperation(final Project project, final User user)
    {
        return true;
    }

}
