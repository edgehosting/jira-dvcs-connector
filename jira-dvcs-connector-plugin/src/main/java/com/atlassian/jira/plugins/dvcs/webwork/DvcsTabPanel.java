package com.atlassian.jira.plugins.dvcs.webwork;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.tabpanels.GenericMessageAction;
import com.atlassian.jira.plugin.issuetabpanel.AbstractIssueTabPanel;
import com.atlassian.jira.plugin.issuetabpanel.IssueAction;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFile;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.util.DvcsConstants;
import com.atlassian.jira.plugins.dvcs.util.VelocityUtils;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.template.soy.SoyTemplateRendererProvider;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.soy.renderer.SoyException;
import com.atlassian.soy.renderer.SoyTemplateRenderer;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DvcsTabPanel extends AbstractIssueTabPanel
{
    /**
     * Represents advertisement content of commit tab panel shown when no repository is linked.
     * 
     * @author Stanislav Dvorscak
     * 
     */
    private final class AdvertisementAction implements IssueAction
    {

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isDisplayActionAllTab()
        {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Date getTimePerformed()
        {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getHtml()
        {
            try
            {
                webResourceManager.requireResourcesForContext("com.atlassian.jira.plugins.jira-bitbucket-connector-plugin");
                return soyTemplateRenderer.render(DvcsConstants.SOY_TEMPLATE_KEY, "jira.dvcs.connector.plugin.soy.advertisement",
                        Collections.<String, Object> emptyMap());
            } catch (SoyException e)
            {
                logger.error("Unable to do appropriate rendering!", e);
                return "";
            }
        }
    }

    private final Logger logger = LoggerFactory.getLogger(DvcsTabPanel.class);

    private static final GenericMessageAction DEFAULT_MESSAGE = new GenericMessageAction("No commits found.");
    private final ApplicationProperties applicationProperties;
    private final PermissionManager permissionManager;
    private final ChangesetService changesetService;
    private final RepositoryService repositoryService;

    private final IssueLinker issueLinker;
    private final TemplateRenderer templateRenderer;
    private final SoyTemplateRenderer soyTemplateRenderer;
    private final WebResourceManager webResourceManager;

    public DvcsTabPanel(PermissionManager permissionManager, ChangesetService changesetService,
            ApplicationProperties applicationProperties, IssueLinker issueLinker, TemplateRenderer templateRenderer,
            SoyTemplateRendererProvider soyTemplateRendererProvider, RepositoryService repositoryService,
            WebResourceManager webResourceManager)
    {
        this.permissionManager = permissionManager;
        this.changesetService = changesetService;
        this.applicationProperties = applicationProperties;
        this.issueLinker = issueLinker;
        this.templateRenderer = templateRenderer;
        this.soyTemplateRenderer = soyTemplateRendererProvider.getRenderer();
        this.repositoryService = repositoryService;
        this.webResourceManager = webResourceManager;
    }

    @Override
    public List<IssueAction> getActions(Issue issue, User user)
    {
        // make advertisement, if plug-in is not using
        if (!repositoryService.existsLinkedRepositories())
        {
            return Collections.<IssueAction> singletonList(new AdvertisementAction());
        }

        String issueKey = issue.getKey();
        List<IssueAction> bitbucketActions = new ArrayList<IssueAction>();
        try
        {
            Map<String, List<Changeset>> changesetsGroupedByNode = new LinkedHashMap<String, List<Changeset>>();

            final List<Changeset> changesetList = changesetService.getByIssueKey(issueKey);
            for (Changeset changeset : changesetList)
            {
                logger.debug("found changeset [ {} ] on issue [ {} ]", changeset.getNode(), issueKey);
                String node = changeset.getNode();
                if (changesetsGroupedByNode.containsKey(node))
                {
                    changesetsGroupedByNode.get(node).add(changeset);
                } else
                {
                    List<Changeset> changesetsWithSameNode = Lists.newArrayList();
                    changesetsWithSameNode.add(changeset);
                    changesetsGroupedByNode.put(node, changesetsWithSameNode);
                }
            }

            for (String node : changesetsGroupedByNode.keySet())
            {
                List<Changeset> changesetsWithSameNode = changesetsGroupedByNode.get(node);

                String changesetAsHtml = getHtmlForChangeset(changesetsWithSameNode);
                if (StringUtils.isNotBlank(changesetAsHtml))
                {
                    bitbucketActions.add(new CommitsIssueAction(changesetAsHtml, changesetsWithSameNode.get(0).getDate()));
                }
            }

        } catch (SourceControlException e)
        {
            logger.debug("Could not retrieve changeset for [ " + issueKey + " ]: " + e, e);
        }

        if (bitbucketActions.isEmpty())
            bitbucketActions.add(DEFAULT_MESSAGE);

        return bitbucketActions;
    }

    @Override
    public boolean showPanel(Issue issue, User user)
    {
        return permissionManager.hasPermission(Permissions.VIEW_VERSION_CONTROL, issue, user);
    }

    public String getHtmlForChangeset(List<Changeset> changesets)
    {

        Map<String, Object> templateMap = new HashMap<String, Object>();

        templateMap.put("velocity_utils", new VelocityUtils());
        templateMap.put("issue_linker", issueLinker);


        String documentJpgUrl = applicationProperties.getBaseUrl()
                + "/download/resources/com.atlassian.jira.plugins.jira-bitbucket-connector-plugin/images/document.jpg";
        templateMap.put("document_jpg_url", documentJpgUrl);


        List<Repository> repositories = Lists.newArrayList();

        Map<Repository, String> commitUrlsByRepo = Maps.newHashMap();
        Map<Repository, Map<ChangesetFile, String>> fileCommitUrlsByRepo = Maps.newHashMap();
        for (Changeset changeset : changesets)
        {
            Repository repository = repositoryService.get(changeset.getRepositoryId());
            if (repository == null || repository.isDeleted() || !repository.isLinked())
            {
                continue;
            }

            repositories.add(repository);


            String commitUrl = changesetService.getCommitUrl(repository, changeset);
            commitUrlsByRepo.put(repository, commitUrl);


            Map<ChangesetFile, String> fileCommitUrls = changesetService.getFileCommitUrls(repository, changeset);
            fileCommitUrlsByRepo.put(repository, fileCommitUrls);
        }

        // all repositories which are associated with given changesets is deleted or unlinked
        if (repositories.isEmpty()) {
            return null;
        }

        Changeset firstChangeset = changesets.get(0);
        Repository firstRepository = repositories.get(0);

        String authorName = firstChangeset.getRawAuthor();
        String login = firstChangeset.getAuthor();

        templateMap.put("changeset", firstChangeset);
        templateMap.put("repositories", repositories);
        templateMap.put("commit_urls_by_repo", commitUrlsByRepo);
        templateMap.put("file_commit_urls_by_repo", fileCommitUrlsByRepo);


        DvcsUser user = repositoryService.getUser(firstRepository, firstChangeset.getAuthor(), firstChangeset.getRawAuthor());

        String commitMessage = firstChangeset.getMessage();

        templateMap.put("gravatar_url", user.getAvatar());
        templateMap.put("user_url", user.getUrl());
        templateMap.put("login", login);
        templateMap.put("user_name", authorName);
        templateMap.put("commit_message", commitMessage);
        templateMap.put("commit_hash", firstChangeset.getNode());
        templateMap.put("max_visible_files", Changeset.MAX_VISIBLE_FILES);

        StringWriter sw = new StringWriter();
        try
        {
            templateRenderer.render("/templates/commits-view.vm", templateMap, sw);
        } catch (IOException e)
        {
            logger.warn(e.getMessage(), e);
        }
        return sw.toString();
    }

}
