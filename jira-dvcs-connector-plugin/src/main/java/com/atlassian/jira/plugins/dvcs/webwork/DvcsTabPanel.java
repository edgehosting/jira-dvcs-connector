package com.atlassian.jira.plugins.dvcs.webwork;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.config.FeatureManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugin.issuetabpanel.AbstractIssueTabPanel;
import com.atlassian.jira.plugin.issuetabpanel.IssueAction;
import com.atlassian.jira.plugins.dvcs.analytics.DvcsCommitsAnalyticsEvent;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.githubenterprise.GithubEnterpriseCommunicator;
import com.atlassian.jira.plugins.dvcs.util.DvcsConstants;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.template.soy.SoyTemplateRendererProvider;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.ApplicationUsers;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.atlassian.soy.renderer.SoyException;
import com.atlassian.soy.renderer.SoyTemplateRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public class DvcsTabPanel extends AbstractIssueTabPanel
{
    public static final String LABS_OPT_IN = "jira.plugin.devstatus.phasetwo";

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
    private final OrganizationService organizationService;

    private final SoyTemplateRenderer soyTemplateRenderer;
    private final WebResourceManager webResourceManager;

    private final ChangesetRenderer renderer;

    private final EventPublisher eventPublisher;
    private final FeatureManager featureManager;

    public DvcsTabPanel(PermissionManager permissionManager,
            SoyTemplateRendererProvider soyTemplateRendererProvider, RepositoryService repositoryService,
            WebResourceManager webResourceManager, ChangesetRenderer renderer, EventPublisher eventPublisher,
            FeatureManager featureManager, OrganizationService organizationService)
    {
        this.permissionManager = permissionManager;
        this.renderer = renderer;
        this.soyTemplateRenderer = soyTemplateRendererProvider.getRenderer();
        this.repositoryService = repositoryService;
        this.webResourceManager = webResourceManager;
        this.eventPublisher = eventPublisher;
        this.featureManager = featureManager;
        this.organizationService = organizationService;
    }

    @Override
    public List<IssueAction> getActions(Issue issue, User user)
    {
        // make advertisement, if plug-in is not using
        if (!repositoryService.existsLinkedRepositories())
        {
            eventPublisher.publish(new DvcsCommitsAnalyticsEvent("issue", "tabshowing", false));
            return Collections.<IssueAction> singletonList(new AdvertisementAction());
        }

        List<IssueAction> actions = renderer.getAsActions(issue);
        if (actions.isEmpty())
        {
            actions.add(ChangesetRenderer.DEFAULT_MESSAGE);
        }

        eventPublisher.publish(new DvcsCommitsAnalyticsEvent("issue", "tabshowing", true));
        return actions;
    }

    @Override
    public boolean showPanel(Issue issue, User user)
    {
        ApplicationUser auser = ApplicationUsers.from(user);
        return (permissionManager.hasPermission(Permissions.VIEW_VERSION_CONTROL, issue, user)
                && (!featureManager.isEnabledForUser(auser,LABS_OPT_IN) || isGithubConnected()));
    }

    private boolean isGithubConnected()
    {
        return organizationService.existsOrganizationWithType(GithubCommunicator.GITHUB, GithubEnterpriseCommunicator.GITHUB_ENTERPRISE);
    }
}
