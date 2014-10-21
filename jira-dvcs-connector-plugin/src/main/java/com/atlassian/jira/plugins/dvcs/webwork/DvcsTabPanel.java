package com.atlassian.jira.plugins.dvcs.webwork;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugin.issuetabpanel.AbstractIssueTabPanel;
import com.atlassian.jira.plugin.issuetabpanel.IssueAction;
import com.atlassian.jira.plugins.dvcs.analytics.DvcsCommitsAnalyticsEvent;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.util.DvcsConstants;
import com.atlassian.jira.template.soy.SoyTemplateRendererProvider;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.atlassian.soy.renderer.SoyException;
import com.atlassian.soy.renderer.SoyTemplateRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Date;
import java.util.List;

@Scanned
public class DvcsTabPanel extends AbstractIssueTabPanel
{

    /**
     * Represents advertisement content of commit tab panel shown when no repository is linked.
     *
     * @author Stanislav Dvorscak
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
                        Collections.<String, Object>emptyMap());
            }
            catch (SoyException e)
            {
                logger.error("Unable to do appropriate rendering!", e);
                return "";
            }
        }

    }

    private final Logger logger = LoggerFactory.getLogger(DvcsTabPanel.class);

    private final PanelVisibilityManager panelVisibilityManager;

    private final RepositoryService repositoryService;

    private final SoyTemplateRenderer soyTemplateRenderer;
    private final WebResourceManager webResourceManager;

    private final ChangesetRenderer renderer;

    private final EventPublisher eventPublisher;

    public DvcsTabPanel(PanelVisibilityManager panelVisibilityManager,
            @ComponentImport SoyTemplateRendererProvider soyTemplateRendererProvider, RepositoryService repositoryService,
            @ComponentImport WebResourceManager webResourceManager,
            ChangesetRenderer renderer, @ComponentImport EventPublisher eventPublisher)
    {
        this.panelVisibilityManager = panelVisibilityManager;
        this.renderer = renderer;
        this.soyTemplateRenderer = soyTemplateRendererProvider.getRenderer();
        this.repositoryService = repositoryService;
        this.webResourceManager = webResourceManager;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public List<IssueAction> getActions(Issue issue, User user)
    {
        // make advertisement, if plug-in is not using
        if (!repositoryService.existsLinkedRepositories())
        {
            eventPublisher.publish(new DvcsCommitsAnalyticsEvent("issue", "tabshowing", false));
            return Collections.<IssueAction>singletonList(new AdvertisementAction());
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
        return panelVisibilityManager.showPanel(issue, user);
    }

}
