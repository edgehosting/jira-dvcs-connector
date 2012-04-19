package com.atlassian.jira.plugins.bitbucket.streams;

import com.atlassian.jira.plugins.bitbucket.DvcsRepositoryManager;
import com.atlassian.jira.plugins.bitbucket.activeobjects.v2.IssueMapping;
import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.IssueLinker;
import com.atlassian.jira.plugins.bitbucket.api.RepositoryManager;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlUser;
import com.atlassian.jira.plugins.bitbucket.api.streams.GlobalFilter;
import com.atlassian.jira.plugins.bitbucket.api.util.CustomStringUtils;
import com.atlassian.jira.plugins.bitbucket.velocity.VelocityUtils;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.streams.api.*;
import com.atlassian.streams.api.common.ImmutableNonEmptyList;
import com.atlassian.streams.api.common.Option;
import com.atlassian.streams.spi.*;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.atlassian.util.concurrent.Nullable;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.apache.commons.collections.CollectionUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class BitbucketStreamsActivityProvider implements StreamsActivityProvider
{

    private I18nResolver i18nResolver;
    private ApplicationProperties applicationProperties;
    private UserProfileAccessor userProfileAccessor;
    private RepositoryManager globalRepositoryManager;

    private static final Logger log = LoggerFactory.getLogger(BitbucketStreamsActivityProvider.class);
    private final IssueLinker issueLinker;
    private final TemplateRenderer templateRenderer;
    private final PermissionManager permissionManager;
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final ProjectManager projectManager;

    public BitbucketStreamsActivityProvider(I18nResolver i18nResolver, ApplicationProperties applicationProperties,
                                            UserProfileAccessor userProfileAccessor, @Qualifier("globalRepositoryManager") RepositoryManager globalRepositoryManager, IssueLinker issueLinker, TemplateRenderer templateRenderer, PermissionManager permissionManager, JiraAuthenticationContext jiraAuthenticationContext, ProjectManager projectManager)
    {
        this.applicationProperties = applicationProperties;
        this.i18nResolver = i18nResolver;
        this.userProfileAccessor = userProfileAccessor;
        this.globalRepositoryManager = globalRepositoryManager;
        this.issueLinker = issueLinker;
        this.templateRenderer = templateRenderer;
        this.permissionManager = permissionManager;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.projectManager = projectManager;
    }

    public Iterable<StreamsEntry> transformEntries(Iterable<Changeset> changesetEntries, AtomicBoolean cancelled) throws StreamsException
    {
        List<StreamsEntry> entries = new ArrayList<StreamsEntry>();
        for (Changeset changeset : changesetEntries)
        {
            if (cancelled.get())
                throw new CancelledException();
            entries.add(toStreamsEntry(changeset));
        }
        return entries;
    }

    /**
     * Transforms a single {@link IssueMapping} to a {@link StreamsEntry}.
     *
     * @param changeset the changeset entry
     * @return the transformed streams entry
     */
    private StreamsEntry toStreamsEntry(final Changeset changeset)
    {
        StreamsEntry.ActivityObject activityObject = new StreamsEntry.ActivityObject(StreamsEntry.ActivityObject.params()
                .id(changeset.getNode()).alternateLinkUri(URI.create(""))
                .activityObjectType(ActivityObjectTypes.status()));

        final String author = changeset.getAuthor();
        final SourceControlRepository repo = globalRepositoryManager.getRepository(changeset.getRepositoryId());
        final String changeSetCommitUrl = repo.getRepositoryUri().getCommitUrl(changeset.getNode());

        StreamsEntry.Renderer renderer = new StreamsEntry.Renderer()
        {
            public Html renderTitleAsHtml(StreamsEntry entry)
            {

                Map<String, Object> templateMap = new HashMap<String, Object>();
                templateMap.put("changeset", changeset);
                templateMap.put("user_name", changeset.getRawAuthor());
                templateMap.put("login", author);
                templateMap.put("user_url", repo.getRepositoryUri().getUserUrl(CustomStringUtils.encodeUriPath(author)));
				templateMap.put("commit_url", changeSetCommitUrl);

                StringWriter sw = new StringWriter();
                try
                {
                    templateRenderer.render("/templates/com/atlassian/jira/plugins/bitbucket/streams/activityentry-title.vm", templateMap, sw);
                } catch (IOException e)
                {
                    log.warn(e.getMessage(), e);
                }

                return new Html(sw.toString());
            }

            public Option<Html> renderSummaryAsHtml(StreamsEntry entry)
            {
                return Option.none();
            }

            public Option<Html> renderContentAsHtml(StreamsEntry entry)
            {
                Map<String, Object> templateMap = new HashMap<String, Object>();
                templateMap.put("velocity_utils", new VelocityUtils());
                templateMap.put("issue_linker", issueLinker);
                templateMap.put("changeset", changeset);
                templateMap.put("repository", globalRepositoryManager.getRepository(changeset.getRepositoryId()));
                templateMap.put("commit_url", changeSetCommitUrl);
                templateMap.put("max_visible_files", DvcsRepositoryManager.MAX_VISIBLE_FILES);

                StringWriter sw = new StringWriter();
                try
                {
                    templateRenderer.render("/templates/com/atlassian/jira/plugins/bitbucket/streams/activityentry-summary.vm", templateMap, sw);
                } catch (IOException e)
                {
                    log.warn(e.getMessage(), e);
                }
                return Option.some(new Html(sw.toString()));
            }

        };

        UserProfile userProfile = userProfileAccessor.getAnonymousUserProfile();

        SourceControlUser user = globalRepositoryManager.getUser(repo, changeset.getAuthor());
        if (user != null && user.getAvatar() != null && user.getAvatar().startsWith("https"))
        {
            try {
                URI uri = new URI(user.getAvatar());
                userProfile = new UserProfile.Builder("").profilePictureUri(Option.option(uri)).build();
            } catch (URISyntaxException e) {
                // do nothing. we use anonymous gravatar
            }
        }



        return new StreamsEntry(StreamsEntry.params()
                .id(URI.create(""))
                .postedDate(new DateTime(changeset.getTimestamp()))
                .authors(ImmutableNonEmptyList.of(userProfile))
                .addActivityObject(activityObject)
                .verb(ActivityVerbs.update())
                .alternateLinkUri(URI.create(""))
                .renderer(renderer)
                .applicationType(applicationProperties.getDisplayName()), i18nResolver);
    }


    public CancellableTask<StreamsFeed> getActivityFeed(final ActivityRequest activityRequest) throws StreamsException
    {
        final GlobalFilter gf = new GlobalFilter();
        //get all changeset entries that match the specified activity filters
        gf.setInProjects(getInProjectsByPermission(Filters.getIsValues(activityRequest.getStandardFilters().get(StandardStreamsFilterOption.PROJECT_KEY))));
        gf.setNotInProjects(Filters.getNotValues(activityRequest.getStandardFilters().get(StandardStreamsFilterOption.PROJECT_KEY)));
        gf.setInUsers(Filters.getIsValues(activityRequest.getStandardFilters().get(StandardStreamsFilterOption.USER.getKey())));
        gf.setNotInUsers(Filters.getNotValues(activityRequest.getStandardFilters().get(StandardStreamsFilterOption.USER.getKey())));
        gf.setInIssues(Filters.getIsValues(activityRequest.getStandardFilters().get(StandardStreamsFilterOption.ISSUE_KEY.getKey())));
        gf.setNotInIssues(Filters.getNotValues(activityRequest.getStandardFilters().get(StandardStreamsFilterOption.ISSUE_KEY.getKey())));
        log.debug("GlobalFilter: " + gf);

        return new CancellableTask<StreamsFeed>()
        {
            private AtomicBoolean cancelled = new AtomicBoolean(false);

            @Override
            public StreamsFeed call() throws Exception
            {
                Iterable<StreamsEntry> streamEntries = new ArrayList<StreamsEntry>();
                if (gf.getInProjects() != null && gf.getInProjects().iterator().hasNext())
                {
                    Iterable<Changeset> changesetEntries = globalRepositoryManager.getLatestChangesets(activityRequest.getMaxResults(), gf);
                    if (cancelled.get())
                        throw new CancelledException();
                    log.debug("Found changeset entries: " + changesetEntries);
                    streamEntries = transformEntries(changesetEntries, cancelled);
                }
                return new StreamsFeed(i18nResolver.getText("streams.external.feed.title"), streamEntries, Option.<String>none());
            }

            @Override
            public Result cancel()
            {
                cancelled.set(true);
                return Result.CANCELLED;
            }
        };
    }

    private Iterable<String> getInProjectsByPermission(Set<String> inProjectsList)
    {
        Iterable<Project> projectsToCheckPermission;

        if (CollectionUtils.isEmpty(inProjectsList))
        {
            projectsToCheckPermission = projectManager.getProjectObjects();
        } else
        {
            projectsToCheckPermission = Iterables.transform(inProjectsList, projectKeyToProject);
        }

        return Iterables.transform(Iterables.filter(projectsToCheckPermission, hasViewSourcePermissionForProject), projectToProjectKey);
    }

    private final Predicate<Project> hasViewSourcePermissionForProject = new Predicate<Project>()
    {
        @Override
        public boolean apply(@Nullable Project project)
        {
            return project != null && permissionManager.hasPermission(Permissions.VIEW_VERSION_CONTROL, project, jiraAuthenticationContext.getLoggedInUser());
        }
    };

    private Function<Project, String> projectToProjectKey = new Function<Project, String>()
    {
        @Override
        public String apply(@Nullable Project from)
        {
            return from.getKey();
        }
    };

    private Function<String, Project> projectKeyToProject = new Function<String, Project>()
    {
        @Override
        public Project apply(@Nullable String from)
        {
            return projectManager.getProjectObjByKey(from);
        }
    };
}