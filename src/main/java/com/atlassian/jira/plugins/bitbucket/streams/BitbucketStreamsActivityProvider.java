package com.atlassian.jira.plugins.bitbucket.streams;

import static com.atlassian.jira.plugins.bitbucket.streams.BitbucketFilterOptionProvider.BitbucketUPMActivityObjectTypes.*;

import java.net.URI;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import com.atlassian.jira.plugins.bitbucket.activeobjects.v2.ChangesetMapping;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryManager;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.streams.api.ActivityRequest;
import com.atlassian.streams.api.ActivityVerb;
import com.atlassian.streams.api.StreamsEntry;
import com.atlassian.streams.api.StreamsException;
import com.atlassian.streams.api.StreamsFeed;
import com.atlassian.streams.api.UserProfile;
import com.atlassian.streams.api.common.ImmutableNonEmptyList;
import com.atlassian.streams.api.common.Option;
import com.atlassian.streams.spi.StreamsActivityProvider;
import com.atlassian.streams.spi.UserProfileAccessor;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;

public class BitbucketStreamsActivityProvider implements StreamsActivityProvider
{

    private final I18nResolver i18nResolver;
    private final ApplicationProperties applicationProperties;
    private final UserProfileAccessor userProfileAccessor;
    private final RepositoryManager globalRepositoryManager;

    private static final Logger log = LoggerFactory.getLogger(BitbucketStreamsActivityProvider.class);

    public BitbucketStreamsActivityProvider(I18nResolver i18nResolver, ApplicationProperties applicationProperties,
        UserProfileAccessor userProfileAccessor, @Qualifier("globalRepositoryManager") RepositoryManager globalRepositoryManager)
    {
        this.applicationProperties = applicationProperties;
        this.i18nResolver = i18nResolver;
        this.userProfileAccessor = userProfileAccessor;
        this.globalRepositoryManager = globalRepositoryManager;
    }

    public Iterable<StreamsEntry> transformEntries(Iterable<ChangesetMapping> changesetEntries) throws StreamsException
    {
//        new Activity.Builder(application("Bitbucket commits", URI.create(applicationProperties.getBaseUrl())),
//                new DateTime(),
//                new UserProfile.Builder(username).fullName(username)
//                        .build())
//                .content(some(html("kontent...")))
//                .title(some(html(username + " commited on " + issueKey)))
//                .url(some(URI.create(applicationProperties.getBaseUrl())))
//                .build();
//        for (Activity activity : result.right()) {
//            activityService.postActivity(activity);
//        }
//        for (ValidationErrors errors : result.left())
//            log.error("Errors encountered attempting to post activity: " + errors.toString());

        return Iterables.transform(changesetEntries, new Function<ChangesetMapping, StreamsEntry>()
        {
            @Override
            public StreamsEntry apply(ChangesetMapping from)
            {
                log.debug("Transforming changeset" + from.getNode());
                return toStreamsEntry(from);
            }
        });
    }

    /**
     * Transforms a single {@link AuditLogEntry} to a {@link StreamsEntry}.
     *
     * @param changesetEntry the log entry
     * @return the transformed streams entry
     */
    private StreamsEntry toStreamsEntry(final ChangesetMapping changesetEntry)
    {
        final URI issueUri = URI.create(applicationProperties.getBaseUrl() + "/browse/" + changesetEntry.getIssueId());

        StreamsEntry.ActivityObject activityObject = new StreamsEntry.ActivityObject(StreamsEntry.ActivityObject.params().id("")
            .alternateLinkUri(URI.create("")).activityObjectType(bitbucketEvent()));

        final UserProfile userProfile = userProfileAccessor.getUserProfile(changesetEntry.getAuthor());

        StreamsEntry.Renderer renderer = new StreamsEntry.Renderer()
        {
            @Override
            public StreamsEntry.Html renderTitleAsHtml(StreamsEntry entry)
            {
                String userHtml = (userProfile.getProfilePageUri().isDefined()) ? "<a href=\"" + userProfile.getProfilePageUri().get()
                    + "\"  class=\"activity-item-user activity-item-author\">" + userProfile.getUsername() + "</a>" : userProfile
                    .getUsername();
                return new StreamsEntry.Html(userHtml + " committed changeset " + changesetEntry.getNode() + " to the " + "<a href=\""
                    + issueUri + "\">" + changesetEntry.getIssueId() + "</a>" + " issue saying:");
            }

            @Override
            public Option<StreamsEntry.Html> renderSummaryAsHtml(StreamsEntry entry)
            {
                return Option.none();
            }

            @Override
            public Option<StreamsEntry.Html> renderContentAsHtml(StreamsEntry entry)
            {
                return Option.some(new StreamsEntry.Html(changesetEntry.getMessage()));
            }
        };

//        ActivityVerb verb = BitbucketFilterOptionProvider.BitbucketUPMActivityVerbs.getVerbFromEntryType(changesetEntry.getEntryType());
        ActivityVerb verb = BitbucketFilterOptionProvider.BitbucketUPMActivityVerbs.getVerb("test-verb1");

        StreamsEntry streamsEntry = new StreamsEntry(StreamsEntry.params().id(issueUri).postedDate(
            new DateTime(changesetEntry.getTimestamp().getTime())).authors(ImmutableNonEmptyList.of(userProfile)).addActivityObject(
            activityObject).verb(verb)
//                .addLink(URI.create(webResourceManager.getStaticPluginResource(
//                        "com.atlassian.streams.external-provider-sample:externalProviderWebResources",
//                        "puzzle-piece.gif",
//                        UrlMode.ABSOLUTE)),
//                        StreamsActivityProvider.ICON_LINK_REL,
//                        none(String.class))
            .alternateLinkUri(issueUri).renderer(renderer).applicationType(applicationProperties.getDisplayName()), i18nResolver);
        return streamsEntry;
    }

    @Override
    public StreamsFeed getActivityFeed(ActivityRequest activityRequest) throws StreamsException
    {
        // get all audit log entries that match the specified activity filters
        Iterable<ChangesetMapping> changesetEntries = globalRepositoryManager.getLastChangesetMappings(activityRequest.getMaxResults());
        Iterable<StreamsEntry> auditLogEntries = transformEntries(changesetEntries);
        return new StreamsFeed(i18nResolver.getText("streams.external.feed.title"), auditLogEntries, Option.<String> none());
    }
}