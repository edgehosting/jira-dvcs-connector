package com.atlassian.jira.plugins.bitbucket.streams;

import com.atlassian.jira.plugins.bitbucket.activeobjects.v2.IssueMapping;
import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.ChangesetFile;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryManager;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.streams.api.*;
import com.atlassian.streams.api.common.ImmutableNonEmptyList;
import com.atlassian.streams.api.common.Option;
import com.atlassian.streams.spi.Filters;
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
import java.util.*;

public class BitbucketStreamsActivityProvider implements StreamsActivityProvider
{

    private I18nResolver i18nResolver;
    private ApplicationProperties applicationProperties;
    private UserProfileAccessor userProfileAccessor;
    private RepositoryManager globalRepositoryManager;

    private static final Logger log = LoggerFactory.getLogger(BitbucketStreamsActivityProvider.class);


    public BitbucketStreamsActivityProvider(I18nResolver i18nResolver, ApplicationProperties applicationProperties, UserProfileAccessor userProfileAccessor, @Qualifier("globalRepositoryManager") RepositoryManager globalRepositoryManager)
    {
        this.applicationProperties = applicationProperties;
        this.i18nResolver = i18nResolver;
        this.userProfileAccessor = userProfileAccessor;
        this.globalRepositoryManager = globalRepositoryManager;
    }


    public Iterable<StreamsEntry> transformEntries(Iterable<IssueMapping> changesetEntries) throws StreamsException
    {
        return Iterables.transform(changesetEntries,
                new Function<IssueMapping, StreamsEntry>()
                {
                    public StreamsEntry apply(IssueMapping from)
                    {
                        return toStreamsEntry(from);
                    }
                });
    }

    /**
     * Transforms a single {@link IssueMapping} to a {@link StreamsEntry}.
     *
     * @param changesetEntry the changeset entry
     * @return the transformed streams entry
     */
    private StreamsEntry toStreamsEntry(final IssueMapping changesetEntry)
    {
        final URI issueUri = URI.create(applicationProperties.getBaseUrl() + "/browse/" + changesetEntry.getIssueId());

        StreamsEntry.ActivityObject activityObject = new StreamsEntry.ActivityObject(StreamsEntry.ActivityObject.params()
                .id(changesetEntry.getNode()).alternateLinkUri(URI.create(""))
                .activityObjectType(ActivityObjectTypes.status()));

        final UserProfile userProfile = userProfileAccessor.getUserProfile(changesetEntry.getAuthor());

        StreamsEntry.Renderer renderer = new StreamsEntry.Renderer()
        {
            public StreamsEntry.Html renderTitleAsHtml(StreamsEntry entry)
            {
                SourceControlRepository repo = globalRepositoryManager.getRepository(changesetEntry.getRepositoryId());
                String userHtml = (userProfile.getProfilePageUri().isDefined()) ? "<a href=\"" + userProfile.getProfilePageUri().get() + "\"  class=\"activity-item-user activity-item-author\">" + userProfile.getUsername() + "</a>" : TextUtils.htmlEncode(userProfile.getUsername());
                return new StreamsEntry.Html(userHtml + " committed changeset <a href=\"" + repo.getRepositoryUri().getCommitUrl(changesetEntry.getNode()) + "\">" + changesetEntry.getNode() + "</a> to the " +
                        "<a href=\"" + issueUri + "\">" + changesetEntry.getIssueId() + "</a>" + " issue saying:");
            }

            public Option<StreamsEntry.Html> renderSummaryAsHtml(StreamsEntry entry)
            {
                return Option.none();
            }

            public Option<StreamsEntry.Html> renderContentAsHtml(StreamsEntry entry)
            {
                SourceControlRepository repo = globalRepositoryManager.getRepository(changesetEntry.getRepositoryId());

                Map<String, String> mapFiles = new HashMap<String, String>();
                String htmlFile = "";
                Changeset changeset = globalRepositoryManager.getChangeset(changesetEntry.getNode());
                if (!changeset.getFiles().isEmpty())
                {
                    for (ChangesetFile file : changeset.getFiles())
                    {
                        String fileName = file.getFile();
                        String color = file.getFileAction().getColor();
                        String fileActionName = file.getFileAction().toString();
                        String fileCommitURL = repo.getRepositoryUri().getRepositoryUrl() + "/src/" + changeset.getNode() + "/" + urlEncode(file.getFile());
                        htmlFile = "<li><span style='color:" + color + "; font-size: 8pt;'>" +
                                TextUtils.htmlEncode(fileActionName) + "</span> <a href='" +
                                fileCommitURL + "' target='_new'>" + fileName + "</a></li>";
                        mapFiles.put(fileName, htmlFile);
                    }
                }

                String htmlFiles = "";
                String htmlFilesHiddenDescription = "";
                Integer numSeeMore = 0;
                Random randDivID = new Random(System.currentTimeMillis());

                // Sort and compose all files
                Iterator<String> it = mapFiles.keySet().iterator();
                Object obj;

                String htmlHiddenDiv = "";

                if (mapFiles.size() <= 5)
                {
                    while (it.hasNext())
                    {
                        obj = it.next();
                        htmlFiles += mapFiles.get(obj);
                    }
                    htmlFilesHiddenDescription = "";
                } else
                {
                    Integer i = 0;

                    while (it.hasNext())
                    {
                        obj = it.next();

                        if (i <= 4)
                        {
                            htmlFiles += mapFiles.get(obj);
                        } else
                        {
                            htmlHiddenDiv += mapFiles.get(obj);
                        }

                        i++;
                    }

                    numSeeMore = mapFiles.size() - 5;
                    Integer divID = randDivID.nextInt();

                    htmlFilesHiddenDescription = "<div class='see_more' id='see_more_" + divID.toString() + "' style='color: #3C78B5; cursor: pointer; text-decoration: underline;' onclick='toggleMoreFiles(" + divID.toString() + ")'>" +
                            "See " + numSeeMore.toString() + " more" +
                            "</div>" +
                            "<div class='hide_more' id='hide_more_" + divID.toString() + "' style='display: none; color: #3C78B5;  cursor: pointer; text-decoration: underline;' onclick='toggleMoreFiles(" + divID.toString() + ")'>Hide " + numSeeMore.toString() + " Files</div>";

                    htmlHiddenDiv = htmlFilesHiddenDescription + "<div id='" + divID.toString() + "' style='display: none;'><ul>" + htmlHiddenDiv + "</ul></div>";

                }

                StringBuilder sb = new StringBuilder();
                sb.append(getJavascriptForToggling());
                sb.append(changesetEntry.getMessage());
                sb.append("<br>").append("<br>").append("Changes:").append("<br>");
                sb.append("<ul>");
                sb.append(htmlFiles);
                sb.append("</ul>");
                sb.append(htmlHiddenDiv);
                return Option.some(new StreamsEntry.Html(sb.toString()));
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

        StreamsEntry streamsEntry = new StreamsEntry(StreamsEntry.params()
                .id(issueUri)
                .postedDate(new DateTime(changesetEntry.getTimestamp().getTime()))
                .authors(ImmutableNonEmptyList.of(userProfile))
                .addActivityObject(activityObject)
                .verb(ActivityVerbs.update())
//                .addLink(URI.create(webResourceManager.getStaticPluginResource(
//                        "com.atlassian.streams.external-provider-sample:externalProviderWebResources",
//                        "puzzle-piece.gif",
//                        UrlMode.ABSOLUTE)),
//                        StreamsActivityProvider.ICON_LINK_REL,
//                        none(String.class))
                .alternateLinkUri(issueUri)
                .renderer(renderer)
                .applicationType(applicationProperties.getDisplayName()), i18nResolver);
        return streamsEntry;
    }

    public StreamsFeed getActivityFeed(ActivityRequest activityRequest) throws StreamsException
    {
        //get all changeset entries that match the specified activity filters
        Set<String> inProjects = Filters.getIsValues(activityRequest.getProviderFilters().values());
        Set<String> notInProjects = Filters.getNotValues(activityRequest.getProviderFilters().values());

        Iterable<IssueMapping> changesetEntries = globalRepositoryManager.getLastChangesetMappings(activityRequest.getMaxResults(), inProjects, notInProjects);
        Iterable<StreamsEntry> streamEntries = transformEntries(changesetEntries);
        return new StreamsFeed(i18nResolver.getText("streams.external.feed.title"), streamEntries, Option.<String>none());
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