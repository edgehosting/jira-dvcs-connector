package com.atlassian.jira.plugins.bitbucket.streams;

import com.atlassian.jira.plugins.bitbucket.IssueLinker;
import com.atlassian.jira.plugins.bitbucket.activeobjects.v2.IssueMapping;
import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.spi.CustomStringUtils;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryManager;
import com.atlassian.jira.plugins.bitbucket.velocity.VelocityUtils;
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
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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


    public BitbucketStreamsActivityProvider(I18nResolver i18nResolver, ApplicationProperties applicationProperties,
                                            UserProfileAccessor userProfileAccessor, @Qualifier("globalRepositoryManager") RepositoryManager globalRepositoryManager,
                                            IssueLinker issueLinker, TemplateRenderer templateRenderer)
    {
        this.applicationProperties = applicationProperties;
        this.i18nResolver = i18nResolver;
        this.userProfileAccessor = userProfileAccessor;
        this.globalRepositoryManager = globalRepositoryManager;
        this.issueLinker = issueLinker;
        this.templateRenderer = templateRenderer;
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
        final UserProfile userProfile = userProfileAccessor.getAnonymousUserProfile();

        StreamsEntry.Renderer renderer = new StreamsEntry.Renderer()
        {
            public Html renderTitleAsHtml(StreamsEntry entry)
            {
                SourceControlRepository repo = globalRepositoryManager.getRepository(changeset.getRepositoryId());

                Map<String, Object> templateMap = new HashMap<String, Object>();
                templateMap.put("changeset", changeset);
                templateMap.put("user_name", changeset.getRawAuthor());
                templateMap.put("login", author);
                templateMap.put("user_url", repo.getRepositoryUri().getUserUrl(CustomStringUtils.encodeUriPath(author)));
                templateMap.put("commit_url", repo.getRepositoryUri().getCommitUrl(changeset.getNode()));

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
        gf.setInProjects(Filters.getIsValues(activityRequest.getStandardFilters().get(StandardStreamsFilterOption.PROJECT_KEY)));
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
                Iterable<Changeset> changesetEntries = globalRepositoryManager.getLatestChangesets(activityRequest.getMaxResults(), gf);
                if (cancelled.get())
                    throw new CancelledException();
                log.debug("Found changeset entries: " + changesetEntries);
                Iterable<StreamsEntry> streamEntries = transformEntries(changesetEntries, cancelled);
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
}