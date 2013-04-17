package com.atlassian.jira.plugins.dvcs.webwork;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            for (Changeset changeset : changesetService.getByIssueKey(issueKey))
            {
                logger.debug("found changeset [ {} ] on issue [ {} ]", changeset.getNode(), issueKey);
                // SourceControlRepository repository = globalRepositoryManager.getRepository(changeset.getRepositoryId());
                String changesetAsHtml = getHtmlForChangeset(changeset);
                if (StringUtils.isNotBlank(changesetAsHtml))
                {
                    bitbucketActions.add(new CommitsIssueAction(changesetAsHtml, changeset.getDate()));
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

    public String getHtmlForChangeset(Changeset changeset)
    {
        Repository repository = repositoryService.get(changeset.getRepositoryId());
        if (repository == null || repository.isDeleted() || !repository.isLinked())
        {
            return "";
        }

        Map<String, Object> templateMap = new HashMap<String, Object>();
        templateMap.put("velocity_utils", new VelocityUtils());
        templateMap.put("issue_linker", issueLinker);
        templateMap.put("changeset", changeset);

        String documentJpgUrl = applicationProperties.getBaseUrl()
                + "/download/resources/com.atlassian.jira.plugins.jira-bitbucket-connector-plugin/images/document.jpg";
        templateMap.put("document_jpg_url", documentJpgUrl);

        String authorName = changeset.getRawAuthor();
        String login = changeset.getAuthor();

        String commitUrl = changesetService.getCommitUrl(repository, changeset);

        Map<ChangesetFile, String> fileCommitUrls = changesetService.getFileCommitUrls(repository, changeset);
        templateMap.put("file_commit_urls", fileCommitUrls);

        DvcsUser user = repositoryService.getUser(repository, changeset.getAuthor(), changeset.getRawAuthor());

        String commitMessage = changeset.getMessage();

        templateMap.put("gravatar_url", user.getAvatar());
        templateMap.put("user_url", user.getUrl());
        templateMap.put("login", login);
        templateMap.put("user_name", authorName);
        templateMap.put("commit_message", commitMessage);
        templateMap.put("commit_url", commitUrl);
        templateMap.put("commit_hash", changeset.getNode());
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
