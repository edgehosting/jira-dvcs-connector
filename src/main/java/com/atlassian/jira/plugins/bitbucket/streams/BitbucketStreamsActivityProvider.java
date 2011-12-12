package com.atlassian.jira.plugins.bitbucket.streams;

import com.atlassian.jira.plugins.bitbucket.IssueLinker;
import com.atlassian.jira.plugins.bitbucket.activeobjects.v2.IssueMapping;
import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.ChangesetFile;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.spi.CustomStringUtils;
import com.atlassian.jira.plugins.bitbucket.spi.DvcsRepositoryManager;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryManager;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryUri;
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
import com.atlassian.streams.spi.Filters;
import com.atlassian.streams.spi.StandardStreamsFilterOption;
import com.atlassian.streams.spi.StreamsActivityProvider;
import com.atlassian.streams.spi.UserProfileAccessor;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.opensymphony.util.TextUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;

public class BitbucketStreamsActivityProvider implements StreamsActivityProvider
{

    private I18nResolver i18nResolver;
    private ApplicationProperties applicationProperties;
    private UserProfileAccessor userProfileAccessor;
    private RepositoryManager globalRepositoryManager;

    private static final Logger log = LoggerFactory.getLogger(BitbucketStreamsActivityProvider.class);
    private final IssueLinker issueLinker;


    public BitbucketStreamsActivityProvider(I18nResolver i18nResolver, ApplicationProperties applicationProperties,
                                            UserProfileAccessor userProfileAccessor, @Qualifier("globalRepositoryManager") RepositoryManager globalRepositoryManager,
                                            IssueLinker issueLinker)
    {
        this.applicationProperties = applicationProperties;
        this.i18nResolver = i18nResolver;
        this.userProfileAccessor = userProfileAccessor;
        this.globalRepositoryManager = globalRepositoryManager;
        this.issueLinker = issueLinker;
    }

    public Iterable<StreamsEntry> transformEntries(Iterable<Changeset> changesetEntries) throws StreamsException
    {
        return Iterables.transform(changesetEntries, new Function<Changeset, StreamsEntry>()
        {
            public StreamsEntry apply(Changeset from)
            {
                return toStreamsEntry(from);
            }
        });
    }

    /**
     * Transforms a single {@link IssueMapping} to a {@link StreamsEntry}.
     *
     * @param changeset the changeset entry
     * @return the transformed streams entry
     */
    private StreamsEntry toStreamsEntry(final Changeset changeset)
    {
//        final URI issueUri = URI.create(applicationProperties.getBaseUrl() + "/browse/" + changesetEntry.getIssueId());

        StreamsEntry.ActivityObject activityObject = new StreamsEntry.ActivityObject(StreamsEntry.ActivityObject.params()
                .id(changeset.getNode()).alternateLinkUri(URI.create(""))
                .activityObjectType(ActivityObjectTypes.status()));

        final UserProfile userProfile = userProfileAccessor.getUserProfile(changeset.getAuthor());

        StreamsEntry.Renderer renderer = new StreamsEntry.Renderer()
        {
            public Html renderTitleAsHtml(StreamsEntry entry)
            {
                SourceControlRepository repo = globalRepositoryManager.getRepository(changeset.getRepositoryId());
                String userHtml = (userProfile.getProfilePageUri().isDefined()) ? "<a href=\"" + userProfile.getProfilePageUri().get() + "\"  class=\"activity-item-user activity-item-author\">" + userProfile.getUsername() + "</a>" : TextUtils.htmlEncode(userProfile.getUsername());
                return new Html(userHtml + " committed changeset <a href=\"" + repo.getRepositoryUri().getCommitUrl(changeset.getNode()) + "\">" + changeset.getNode() + "</a> saying:");
            }

            public Option<Html> renderSummaryAsHtml(StreamsEntry entry)
            {
                return Option.none();
            }

            public Option<Html> renderContentAsHtml(StreamsEntry entry)
            {
                SourceControlRepository repo = globalRepositoryManager.getRepository(changeset.getRepositoryId());

                RepositoryUri repositoryUri = repo.getRepositoryUri();

                String htmlFiles = "";
                for (int i = 0; i < Math.min(changeset.getFiles().size(), DvcsRepositoryManager.MAX_VISIBLE_FILES); i++)
                {
                    ChangesetFile file = changeset.getFiles().get(i);
                    String fileName = file.getFile();
                    String color = file.getFileAction().getColor();
                    String fileActionName = file.getFileAction().toString();
                    String fileCommitURL = repositoryUri.getFileCommitUrl(changeset.getNode(), CustomStringUtils.encode(file.getFile()));
                    htmlFiles += "<li><span style='color:" + color + "; font-size: 8pt;'>" +
                            TextUtils.htmlEncode(fileActionName) + "</span> <a href='" +
                            fileCommitURL + "' target='_new'>" + fileName + "</a></li>";
                }

                int numSeeMore = changeset.getAllFileCount() - DvcsRepositoryManager.MAX_VISIBLE_FILES;
                if (numSeeMore > 0)
                {
                    htmlFiles += "<div class='see_more' style='margin-top:5px;'><a href='#commit_url' target='_new'>See " + numSeeMore + " more</a></div>";
                }

                StringBuilder sb = new StringBuilder();
                sb.append(getJavascriptForToggling());
                sb.append(issueLinker.createLinks(TextUtils.htmlEncode(changeset.getMessage())));
                sb.append("<br>").append("<br>").append("Changes:").append("<br>");
                sb.append("<ul>");
                sb.append(htmlFiles);
                sb.append("</ul>");
                return Option.some(new Html(sb.toString()));
            }

            private String getJavascriptForToggling()
            {
                return "<script type=\"text/javascript\">" +
                        "function toggleMoreFiles(target_div){\n" +
                        "        AJS.$('#' + target_div).toggle();\n" +
                        "        AJS.$('#see_more_' + target_div).toggle();\n" +
                        "        AJS.$('#hide_more_' + target_div).toggle();\n" +
                        "}\n" +
                        "</script>";
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
            @Override
            public StreamsFeed call() throws Exception
            {
                Iterable<Changeset> changesetEntries = globalRepositoryManager.getLatestChangesets(activityRequest.getMaxResults(), gf);
                log.debug("Found changeset entries: " + changesetEntries);
                Iterable<StreamsEntry> streamEntries = transformEntries(changesetEntries);
                return new StreamsFeed(i18nResolver.getText("streams.external.feed.title"), streamEntries, Option.<String>none());
            }

            @Override
            public Result cancel()
            {
                // TODO Auto-generated method stub
                return null;
            }
        };
    }

    protected String urlEncode(String s)
    {
        try
        {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException("required encoding not found");
        }
    }
}