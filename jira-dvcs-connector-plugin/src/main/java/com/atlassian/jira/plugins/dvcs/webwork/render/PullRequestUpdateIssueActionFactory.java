package com.atlassian.jira.plugins.dvcs.webwork.render;

import java.util.HashMap;
import java.util.Map;

import com.atlassian.jira.plugin.issuetabpanel.IssueAction;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityPullRequestUpdateMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.util.VelocityUtils;
import com.atlassian.jira.plugins.dvcs.webwork.IssueLinker;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.templaterenderer.TemplateRenderer;

public class PullRequestUpdateIssueActionFactory implements IssueActionFactory
{
    private final RepositoryService repositoryService;
    private final IssueLinker issueLinker;
    private final ApplicationProperties applicationProperties;
    private final ChangesetService changesetService;
    private final TemplateRenderer templateRenderer;
    private final RepositoryActivityDao repositoryActivityDao;

    public PullRequestUpdateIssueActionFactory(RepositoryService repositoryService, IssueLinker issueLinker,
            ApplicationProperties applicationProperties, ChangesetService changesetService,
            TemplateRenderer templateRenderer, RepositoryActivityDao repositoryActivityDao)
    {
        this.repositoryService = repositoryService;
        this.issueLinker = issueLinker;
        this.applicationProperties = applicationProperties;
        this.changesetService = changesetService;
        this.templateRenderer = templateRenderer;
        this.repositoryActivityDao = repositoryActivityDao;
    }

    @Override
    public IssueAction create(Object activityItem)
    {
        RepositoryActivityPullRequestUpdateMapping pullRequestUpdate = (RepositoryActivityPullRequestUpdateMapping) activityItem;
        int repositoryId = pullRequestUpdate.getRepositoryId();
        int pullRequestId = pullRequestUpdate.getPullRequestId();
        
        RepositoryPullRequestMapping pullRequest;
        pullRequest = repositoryActivityDao.findRequestById(repositoryId, pullRequestId);
        String pullRequestName = pullRequest.getPullRequestName();

        Map<String, Object> templateMap = new HashMap<String, Object>();
        templateMap.put("velocity_utils", new VelocityUtils());
        templateMap.put("pullRequestUpdate", pullRequestUpdate);
        templateMap.put("pullRequestName", pullRequestName);
        
        return new DefaultIssueAction(templateRenderer, "/templates/activity/pull-request-update-view.vm", templateMap,
                pullRequestUpdate.getLastUpdatedOn());
    }

    @Override
    public Class<? extends Object> getSupportedClass()
    {
        return RepositoryActivityPullRequestUpdateMapping.class;
    }

}
