package com.atlassian.jira.plugins.dvcs.streams;

import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFile;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.GlobalFilter;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.util.VelocityUtils;
import com.atlassian.jira.plugins.dvcs.webwork.IssueAndProjectKeyManager;
import com.atlassian.jira.plugins.dvcs.webwork.IssueLinker;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.streams.api.ActivityObjectTypes;
import com.atlassian.streams.api.ActivityRequest;
import com.atlassian.streams.api.ActivityVerbs;
import com.atlassian.streams.api.Html;
import com.atlassian.streams.api.StreamsEntry;
import com.atlassian.streams.api.StreamsException;
import com.atlassian.streams.api.StreamsFeed;
import com.atlassian.streams.api.UserProfile;
import com.atlassian.streams.api.common.ImmutableNonEmptyList;
import com.atlassian.streams.api.common.Option;
import com.atlassian.streams.spi.CancellableTask;
import com.atlassian.streams.spi.CancelledException;
import com.atlassian.streams.spi.Filters;
import com.atlassian.streams.spi.StandardStreamsFilterOption;
import com.atlassian.streams.spi.StreamsActivityProvider;
import com.atlassian.streams.spi.UserProfileAccessor;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;

@Component
public class DvcsStreamsActivityProvider implements StreamsActivityProvider
{
    private static final Logger log = LoggerFactory.getLogger(DvcsStreamsActivityProvider.class);

    private final I18nResolver i18nResolver;
    private final ApplicationProperties applicationProperties;
    private final UserProfileAccessor userProfileAccessor;
    private final IssueLinker issueLinker;
    private final TemplateRenderer templateRenderer;
    private final PermissionManager permissionManager;
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final ProjectManager projectManager;
    private final ChangesetService changesetService;
    private final RepositoryService repositoryService;
    private final IssueAndProjectKeyManager issueAndProjectKeyManager;

    @Autowired
    public DvcsStreamsActivityProvider(@ComponentImport I18nResolver i18nResolver,
            @ComponentImport ApplicationProperties applicationProperties,
            @ComponentImport UserProfileAccessor userProfileAccessor, IssueLinker issueLinker,
            @ComponentImport TemplateRenderer templateRenderer,
            @ComponentImport PermissionManager permissionManager,
            @ComponentImport JiraAuthenticationContext jiraAuthenticationContext,
            @ComponentImport ProjectManager projectManager, ChangesetService changesetService,
            RepositoryService repositoryService,
            IssueAndProjectKeyManager issueAndProjectKeyManager)
    {
        this.applicationProperties = applicationProperties;
        this.i18nResolver = i18nResolver;
        this.userProfileAccessor = userProfileAccessor;
        this.issueLinker = issueLinker;
        this.templateRenderer = checkNotNull(templateRenderer);
        this.permissionManager = permissionManager;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.projectManager = projectManager;
        this.changesetService = changesetService;
        this.repositoryService = repositoryService;
        this.issueAndProjectKeyManager = issueAndProjectKeyManager;
    }

    private Iterable<StreamsEntry> transformEntries(final ActivityRequest activityRequest, Iterable<Changeset> changesetEntries, AtomicBoolean cancelled) throws StreamsException
    {
        List<StreamsEntry> entries = new ArrayList<StreamsEntry>();
        Set<String> alreadyAddedChangesetRawNodes = new HashSet<String>(entries.size(), 1.0F);

        for (Changeset changeset : changesetEntries)
        {
            if (cancelled.get()) {

                throw new CancelledException();

            } else {
                // https://sdog.jira.com/browse/BBC-308; without this check we would be adding visually same items
                // to activity stream
                if (!alreadyAddedChangesetRawNodes.contains(getNode(changeset)))
                {
                    StreamsEntry streamsEntry = toStreamsEntry(activityRequest, changeset);
                    if (streamsEntry != null)
                    {
                        entries.add(streamsEntry);
                        alreadyAddedChangesetRawNodes.add(getNode(changeset));
                    }
                }
            }
        }
        return entries;
    }

    private String getNode(Changeset changeset)
    {
        String node = changeset.getRawNode();
        // if we don't have raw node e.g. for Github , we use node
        if (StringUtils.isEmpty(node))
        {
            node = changeset.getNode();
        }

        return node;
    }

    /**
     * Transforms a single {@link com.atlassian.jira.plugins.dvcs.activeobjects.v2.IssueMapping} to a {@link com.atlassian.streams.api.StreamsEntry}.
     *
     * @param changeset the changeset entry
     * @return the transformed streams entry
     */
    private StreamsEntry toStreamsEntry(final ActivityRequest activityRequest, final Changeset changeset)
    {
        final Repository repository = repositoryService.get(changeset.getRepositoryId());

        if (repository == null) {
            return null;
        }

        StreamsEntry.ActivityObject activityObject = new StreamsEntry.ActivityObject(StreamsEntry.ActivityObject.params()
                .id(changeset.getNode()).alternateLinkUri(URI.create(""))
                .activityObjectType(ActivityObjectTypes.status()));

        final String commitUrl = changesetService.getCommitUrl(repository, changeset);
        final DvcsUser user = repositoryService.getUser(repository, changeset.getAuthor(), changeset.getRawAuthor());

        StreamsEntry.Renderer renderer = new StreamsEntry.Renderer()
        {
            @Override
            public Html renderTitleAsHtml(StreamsEntry entry)
            {

                Map<String, Object> templateMap = new HashMap<String, Object>();
                templateMap.put("changeset", changeset);
                templateMap.put("user_name", user.getFullName());
                templateMap.put("login", user.getUsername());
                templateMap.put("user_url", user.getUrl());
                templateMap.put("commit_url", commitUrl);

                StringWriter sw = new StringWriter();
                try
                {
                    templateRenderer.render("/templates/activityentry-title.vm", templateMap, sw);
                } catch (IOException e)
                {
                    log.warn(e.getMessage(), e);
                }

                return new Html(sw.toString());
            }

            @Override
            public Option<Html> renderSummaryAsHtml(final URI baseUri, final StreamsEntry entry)
            {
                return renderSummaryAsHtml(entry);
            }

            public Option<Html> renderSummaryAsHtml(StreamsEntry entry)
            {
                return Option.none();
            }


            @Override
            public Option<Html> renderContentAsHtml(final URI baseUri, final StreamsEntry entry)
            {
                return renderContentAsHtml(entry);
            }

            public Option<Html> renderContentAsHtml(StreamsEntry entry)
            {

                Map<String, Object> templateMap = new HashMap<String, Object>();

                Map<ChangesetFile, String> fileCommitUrls = changesetService.getFileCommitUrls(repository, changeset);
                templateMap.put("file_commit_urls", fileCommitUrls);

                templateMap.put("velocity_utils", new VelocityUtils());
                templateMap.put("issue_linker", issueLinker);
                templateMap.put("changeset", changeset);
                templateMap.put("commit_url", commitUrl);
                templateMap.put("max_visible_files", Changeset.MAX_VISIBLE_FILES);

                StringWriter sw = new StringWriter();
                try
                {
                    templateRenderer.render("/templates/activityentry-summary.vm", templateMap, sw);
                } catch (IOException e)
                {
                    log.warn(e.getMessage(), e);
                }
                return Option.some(new Html(sw.toString()));
            }

        };

        UserProfile userProfile = null;

        if (user != null && user.getAvatar() != null && user.getAvatar().startsWith("https"))
        {
            try
            {
                URI uri = new URI(user.getAvatar());
                userProfile = new UserProfile.Builder("").profilePictureUri(Option.option(uri)).build();
            } catch (URISyntaxException e)
            {
                // we use anonymous profile
                userProfile = userProfileAccessor.getAnonymousUserProfile(activityRequest.getContextUri());
            }
        } else
        {
            userProfile = userProfileAccessor.getAnonymousUserProfile(activityRequest.getContextUri());
        }

        return new StreamsEntry(StreamsEntry.params()
                .id(URI.create(""))
                .postedDate(new DateTime(changeset.getDate()))
                .authors(ImmutableNonEmptyList.of(userProfile))
                .addActivityObject(activityObject)
                .verb(ActivityVerbs.update())
                .alternateLinkUri(URI.create(""))
                .renderer(renderer)
                .applicationType(applicationProperties.getDisplayName()), i18nResolver);
    }


    @Override
    public CancellableTask<StreamsFeed> getActivityFeed(final ActivityRequest activityRequest) throws StreamsException
    {
        final GlobalFilter gf = new GlobalFilter();
        //get all changeset entries that match the specified activity filters
        gf.setInProjects(includeHistoricalProjectKeys(getInProjectsByPermission(Filters.getIsValues(activityRequest.getStandardFilters().get(StandardStreamsFilterOption.PROJECT_KEY)))));
        gf.setNotInProjects(includeHistoricalProjectKeys(Filters.getNotValues(activityRequest.getStandardFilters().get(StandardStreamsFilterOption.PROJECT_KEY))));
        gf.setInUsers(Filters.getIsValues(activityRequest.getStandardFilters().get(StandardStreamsFilterOption.USER.getKey())));
        gf.setNotInUsers(Filters.getNotValues(activityRequest.getStandardFilters().get(StandardStreamsFilterOption.USER.getKey())));
        gf.setInIssues(includeHistoricalIssueKeys(Filters.getIsValues(activityRequest.getStandardFilters().get(StandardStreamsFilterOption.ISSUE_KEY.getKey()))));
        gf.setNotInIssues(includeHistoricalIssueKeys(Filters.getNotValues(activityRequest.getStandardFilters().get(StandardStreamsFilterOption.ISSUE_KEY.getKey()))));
        log.debug("GlobalFilter: " + gf);

        return new CancellableTask<StreamsFeed>()
        {
            private final AtomicBoolean cancelled = new AtomicBoolean(false);

            @Override
            public StreamsFeed call() throws Exception
            {
                Iterable<StreamsEntry> streamEntries = new ArrayList<StreamsEntry>();
                if (gf.getInProjects() != null && gf.getInProjects().iterator().hasNext())
                {
                    Iterable<Changeset> latestChangesets = changesetService.getLatestChangesets(activityRequest.getMaxResults(), gf);
                    if (cancelled.get())
                        throw new CancelledException();
                    log.debug("Found changeset entries: {}", latestChangesets);

                    final List<Changeset> changesetDetails = changesetService.getChangesetsWithFileDetails(Lists.newArrayList(latestChangesets));
                    log.debug("Loaded details for changeset entries: {}", changesetDetails);

                    streamEntries = transformEntries(activityRequest, changesetDetails, cancelled);
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

    private Set<String> includeHistoricalProjectKeys(Iterable<String> projectKeys)
    {
        final Set<String> result = new HashSet<String>();
        for (String projectKey : projectKeys)
        {
            result.addAll(issueAndProjectKeyManager.getAllProjectKeys(projectKey));
        }
        return result;
    }

    private Set<String> includeHistoricalIssueKeys(Iterable<String> issueKeys)
    {
        final Set<String> result = new HashSet<String>();
        for (String issueKey : issueKeys)
        {
            result.addAll(issueAndProjectKeyManager.getAllIssueKeys(issueKey));
        }
        return result;
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

    private final Function<Project, String> projectToProjectKey = new Function<Project, String>()
    {
        @Override
        public String apply(@Nullable Project from)
        {
            return from.getKey();
        }
    };

    private final Function<String, Project> projectKeyToProject = new Function<String, Project>()
    {
        @Override
        public Project apply(String from)
        {
            return projectManager.getProjectObjByKey(from);
        }
    };
}
