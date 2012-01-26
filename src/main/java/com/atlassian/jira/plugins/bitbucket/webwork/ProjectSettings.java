package com.atlassian.jira.plugins.bitbucket.webwork;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.plugin.projectoperation.AbstractPluggableProjectOperation;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryManager;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryUri;
import com.atlassian.jira.project.Project;
import com.atlassian.sal.api.ApplicationProperties;

public class ProjectSettings extends AbstractPluggableProjectOperation
{
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
        result.append("<div style=\"padding-bottom:5px; \">");
        result.append("<span class=\"project-config-list-label\">");
        result.append("Bitbucket and GitHub Repositories:");
        result.append("</span>\n");
        result.append("<span class=\"project-config-list-value\">");
        result.append(" (<a href='")
                .append(baseURL)
                .append("/secure/admin/ConfigureBitbucketRepositories!default.jspa?projectKey=")
                .append(project.getKey())
                .append("&mode=single'>")
                .append("Configure</a>)");
        result.append("</span>");
        result.append("</div>");



        if (repositories.isEmpty())
        {
            appendNoneRow(result);
        } else
        {
            for (int i=0; i<repositories.size(); i++) {
                SourceControlRepository repository = repositories.get(0);
                appendRepositoryRow(result, repository);
            }
        }

        return result.toString();

    }

    private void appendNoneRow(StringBuilder result)
    {
        appendRow(result, "None");
    }

    private void appendRepositoryRow(StringBuilder result, SourceControlRepository repository)
    {
        StringBuffer repoRowContent = new StringBuffer();
        RepositoryUri repositoryUri = repository.getRepositoryUri();

        repoRowContent.append(repository.getRepositoryType());
        repoRowContent.append(": ");

        repoRowContent.append("<a href=\"");
        repoRowContent.append(repositoryUri.getRepositoryUrl());
        repoRowContent.append("\" target=\"_new\">");
        repoRowContent.append(repositoryUri.getSlug());
        repoRowContent.append("</a>");

        appendRow(result, repoRowContent.toString());
    }

    private void appendRow(StringBuilder result, String rowText)
    {
        result.append("<div>");
        result.append("<span class=\"project-config-list-label\">");
        result.append(rowText);
        result.append("</span>");
        result.append("</div>");

    }

    @Override
    public boolean showOperation(final Project project, final User user)
    {
        return true;
    }

}
