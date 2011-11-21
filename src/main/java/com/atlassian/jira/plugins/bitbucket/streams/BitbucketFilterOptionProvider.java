package com.atlassian.jira.plugins.bitbucket.streams;

import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.streams.spi.StandardStreamsFilterOption;
import com.atlassian.streams.spi.StreamsFilterOption;
import com.atlassian.streams.spi.StreamsFilterOptionProvider;
import com.atlassian.util.concurrent.Nullable;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class BitbucketFilterOptionProvider implements StreamsFilterOptionProvider
{
    private final I18nResolver i18nResolver;
    private final PermissionManager permissionManager;
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final ProjectManager projectManager;

    public BitbucketFilterOptionProvider(I18nResolver i18nResolver, PermissionManager permissionManager,
                                         JiraAuthenticationContext jiraAuthenticationContext, ProjectManager projectManager)
    {
        this.i18nResolver = checkNotNull(i18nResolver, "i18nResolver");
        this.permissionManager = permissionManager;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.projectManager = projectManager;
    }

    public Iterable<StreamsFilterOption> getFilterOptions()
    {
        return ImmutableList.of(StandardStreamsFilterOption.projectKeys(getProjects(), "bitbucket"));
    }

    private Map<String, String> getProjects()
    {
        return Maps.transformValues(Maps.uniqueIndex(getAllProjects(), toProjectKey), toProjectLabel);
    }

    private final Function<Project, String> toProjectKey = new Function<Project, String>()
    {
        @Override
        public String apply(@Nullable Project project)
        {
            return project.getKey();
        }
    };

    private final Function<Project, String> toProjectLabel = new Function<Project, String>()
    {
        @Override
        public String apply(@Nullable Project project)
        {
            return project.getName();
        }
    };

    private Iterable<Project> getAllProjects()
    {
        return permissionManager.getProjectObjects(Permissions.BROWSE, jiraAuthenticationContext.getLoggedInUser());
    }

    public Iterable<ActivityOption> getActivities()
    {
        return ImmutableList.of();
    }
}
