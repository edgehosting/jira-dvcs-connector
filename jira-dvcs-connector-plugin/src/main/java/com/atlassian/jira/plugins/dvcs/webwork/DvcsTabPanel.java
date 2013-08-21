package com.atlassian.jira.plugins.dvcs.webwork;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugin.issuetabpanel.AbstractIssueTabPanel;
import com.atlassian.jira.plugin.issuetabpanel.IssueAction;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.util.DvcsConstants;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.template.soy.SoyTemplateRendererProvider;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.atlassian.soy.renderer.SoyException;
import com.atlassian.soy.renderer.SoyTemplateRenderer;

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
                return soyTemplateRenderer.render(DvcsConstants.SOY_TEMPLATE_PLUGIN_KEY, "dvcs.connector.plugin.soy.advertisement",
                        Collections.<String, Object> emptyMap());
            } catch (SoyException e)
            {
                logger.error("Unable to do appropriate rendering!", e);
                return "";
            }
        }
    }

    private final Logger logger = LoggerFactory.getLogger(DvcsTabPanel.class);

    private final PermissionManager permissionManager;
    private final RepositoryService repositoryService;

    private final SoyTemplateRenderer soyTemplateRenderer;
    private final WebResourceManager webResourceManager;

    private ChangesetRenderer renderer;

    public DvcsTabPanel(PermissionManager permissionManager,
            SoyTemplateRendererProvider soyTemplateRendererProvider, RepositoryService repositoryService,
            WebResourceManager webResourceManager, ChangesetRenderer renderer)
    {
        this.permissionManager = permissionManager;
        this.renderer = renderer;
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

        List<IssueAction> actions = renderer.getAsActions(issue);
        if (actions.isEmpty()) {
            actions.add(ChangesetRenderer.DEFAULT_MESSAGE);
        }

        return actions;
    }

    @Override
    public boolean showPanel(Issue issue, User user)
    {
        return permissionManager.hasPermission(Permissions.VIEW_VERSION_CONTROL, issue, user);
    }

}
