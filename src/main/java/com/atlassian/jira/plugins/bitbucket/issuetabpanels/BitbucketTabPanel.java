package com.atlassian.jira.plugins.bitbucket.issuetabpanels;

import com.atlassian.jira.config.properties.PropertiesManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.tabpanels.GenericMessageAction;
import com.atlassian.jira.plugin.issuetabpanel.AbstractIssueTabPanel;
import com.atlassian.jira.plugin.issuetabpanel.IssueAction;
import com.atlassian.jira.plugins.bitbucket.bitbucket.*;
import com.atlassian.jira.plugins.bitbucket.mapper.BitbucketMapper;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.opensymphony.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

public class BitbucketTabPanel extends AbstractIssueTabPanel
{

    private final Bitbucket bitbucket;
    private final BitbucketMapper bitbucketMapper;
    private final PermissionManager permissionManager;
    private final Logger logger = LoggerFactory.getLogger(BitbucketTabPanel.class);

    public BitbucketTabPanel(PermissionManager permissionManager, BitbucketMapper bitbucketMapper, Bitbucket bitbucket)
    {
        this.permissionManager = permissionManager;
        this.bitbucketMapper = bitbucketMapper;
        this.bitbucket = bitbucket;
    }

    public List<IssueAction> getActions(Issue issue, User user)
    {
        String issueId = issue.getKey();
        List<IssueAction> bitbucketActions = new ArrayList<IssueAction>();
        for (BitbucketChangeset changeset : bitbucketMapper.getChangesets(issueId))
        {
            logger.debug("found changeset [ " + changeset.getNode() + " ]");
            bitbucketActions.add(new GenericMessageAction(formatCommitDetails(changeset)));
        }

        if (bitbucketActions.isEmpty())
        {
            GenericMessageAction blankAction = new GenericMessageAction("");
            bitbucketActions.add(blankAction);
        }

        return bitbucketActions;
    }

    public boolean showPanel(Issue issue, User user)
    {
        return permissionManager.hasPermission(Permissions.VIEW_VERSION_CONTROL, issue, user);
    }

    private String formatCommitDetails(BitbucketChangeset changeset)
    {
        String baseURL = PropertiesManager.getInstance().getPropertySet().getString("jira.baseurl");

        String authorName = changeset.getRawAuthor();
        String login = changeset.getAuthor();

        String commitURL = "http://bitbucket.org/" + changeset.getRepositoryOwner() + "/" + changeset.getRepositoryOwner() + "/changeset/" + changeset.getNode();

        BitbucketUser bitbucketUser = bitbucket.getUser(changeset.getAuthor());

        String gravatarUrl = bitbucketUser.getAvatar();
        gravatarUrl = gravatarUrl.replace("s=32", "s=60");

        String htmlParentHashes = "";
        if (!changeset.getParents().isEmpty())
        {
            for (String node : changeset.getParents())
            {
                htmlParentHashes = "<tr><td style='color: #757575'>Parent:</td><td><a href='https://bitbucket.org/" +
                        urlEncode(changeset.getRepositoryOwner()) + "/" + urlEncode(changeset.getRepositorySlug()) +
                        "/changeset/" + node + "' target='_new'>" + node + "</a></td></tr>";
            }
        }

        Map<String, String> mapFiles = new HashMap<String, String>();
        String htmlFile = "";
        if (!changeset.getFiles().isEmpty())
        {
            for (BitbucketChangesetFile file : changeset.getFiles())
            {
                String fileName = file.getFile();
                String color = file.getType().getColor();
                String fileActionName = file.getType().toString();
                String fileCommitURL = "https://bitbucket.org/" + changeset.getRepositoryOwner() + "/" +
                        changeset.getRepositorySlug() + "/src/" + changeset.getNode() + "/" + urlEncode(file.getFile());
                htmlFile = "<li><span style='color:" + color + "; font-size: 8pt;'>" +
                        htmlEncode(fileActionName) + "</span> <a href='" +
                        fileCommitURL + "' target='_new'>" + fileName + "</a></li>";
                mapFiles.put(fileName, htmlFile);
            }
        }

        String htmlFiles = "";
        String htmlFilesHiddenDescription = "";
        Integer numSeeMore = 0;
        Random randDivID = new Random(System.currentTimeMillis());

        // Sort and compose all files
        Iterator it = mapFiles.keySet().iterator();
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
        }
        else
        {
            Integer i = 0;

            while (it.hasNext())
            {
                obj = it.next();

                if (i <= 4)
                {
                    htmlFiles += mapFiles.get(obj);
                }
                else
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


        String htmlCommitEntry = "" +
                "<table>" +
                "<tr>" +
                "<td valign='top' width='70px'><a href='#user_url' target='_new'><img src='#gravatar_url' border='0'></a></td>" +
                "<td valign='top'>" +
                "<div style='padding-bottom: 6px'><a href='#user_url' target='_new'>#user_name - #login</a></div>" +
                "<table>" +
                "<tr>" +
                "<td>" +
                "<div style='border-left: 2px solid #C9D9EF; background-color: #EAF3FF; color: #5D5F62; padding: 5px; margin-bottom: 10px;'>#commit_message</div>" +

                "<ul>" +
                htmlFiles +
                "</ul>" +

                htmlHiddenDiv +

                "<div style='margin-top: 10px'>" +
                "<img src='" + baseURL + "/download/resources/com.atlassian.jira.plugins.jira-bitbucket-connector-plugin/images/document.jpg' align='center'> <span class='commit_date' style='color: #757575; font-size: 9pt;'>#formatted_commit_date</span>" +
                "</div>" +

                "</td>" +

                "<td width='400' style='padding-top: 0px' valign='top'>" +
                "<div style='border-left: 2px solid #cccccc; margin-left: 15px; margin-top: 0px; padding-top: 0px; padding-left: 10px'>" +
                "<table style='margin-top: 0px; padding-top: 0px;'>" +
                "<tr><td style='color: #757575'>Changeset:</td><td><a href='#commit_url' target='_new'>#commit_hash</a></td></tr>" +
                htmlParentHashes +
                "</table>" +
                "</div>" +
                "</td>" +

                "</tr>" +
                "</table>" +
                "</td>" +
                "</tr>" +
                "</table>";


        htmlCommitEntry = htmlCommitEntry.replace("#gravatar_url", gravatarUrl);
        htmlCommitEntry = htmlCommitEntry.replace("#user_url", "https://bitbucket.org/" + urlEncode(login));
        htmlCommitEntry = htmlCommitEntry.replace("#login", htmlEncode(login));
        htmlCommitEntry = htmlCommitEntry.replace("#user_name", htmlEncode(authorName));
        htmlCommitEntry = htmlCommitEntry.replace("#commit_message", htmlEncode(changeset.getMessage()));
        htmlCommitEntry = htmlCommitEntry.replace("#formatted_commit_time", changeset.getTimestamp());
        htmlCommitEntry = htmlCommitEntry.replace("#formatted_commit_date", changeset.getTimestamp());
        htmlCommitEntry = htmlCommitEntry.replace("#commit_url", commitURL);
        htmlCommitEntry = htmlCommitEntry.replace("#commit_hash", changeset.getNode());
        //htmlCommitEntry = htmlCommitEntry.replace("#tree_url", "https://github.com/" + login + "/" + projectName + "/tree/" + commit_hash);
        //htmlCommitEntry = htmlCommitEntry.replace("#tree_hash", commitTree);
        return htmlCommitEntry;
    }

    private String htmlEncode(String s)
    {
        JiraWebActionSupport jwas = new JiraWebActionSupport();
        return jwas.htmlEncode(s);
    }


    private String urlEncode(String s)
    {
        try
        {
            return URLEncoder.encode(s, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException("required encoding not found");
        }
    }


}
