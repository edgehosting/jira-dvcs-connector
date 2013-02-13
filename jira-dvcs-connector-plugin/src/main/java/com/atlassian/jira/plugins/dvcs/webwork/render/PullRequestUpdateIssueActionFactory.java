package com.atlassian.jira.plugins.dvcs.webwork.render;

import java.util.HashMap;
import java.util.Map;

import com.atlassian.jira.plugin.issuetabpanel.IssueAction;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityPullRequestUpdateMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.atlassian.jira.plugins.dvcs.util.VelocityUtils;
import com.atlassian.templaterenderer.TemplateRenderer;

public class PullRequestUpdateIssueActionFactory implements IssueActionFactory
{
    private final RepositoryService repositoryService;
    private final TemplateRenderer templateRenderer;
    private final RepositoryActivityDao repositoryActivityDao;
    private final DvcsCommunicatorProvider dvcsCommunicatorProvider;

    public PullRequestUpdateIssueActionFactory(RepositoryService repositoryService, 
            TemplateRenderer templateRenderer, RepositoryActivityDao repositoryActivityDao,
            DvcsCommunicatorProvider dvcsCommunicatorProvider)
    {
        this.repositoryService = repositoryService;
        this.templateRenderer = templateRenderer;
        this.repositoryActivityDao = repositoryActivityDao;
        this.dvcsCommunicatorProvider = dvcsCommunicatorProvider;
    }

    @Override
    public IssueAction create(Object activityItem)
    {
        RepositoryActivityPullRequestUpdateMapping pullRequestUpdate = (RepositoryActivityPullRequestUpdateMapping) activityItem;
        int repositoryId = pullRequestUpdate.getRepoId();
        int pullRequestId = pullRequestUpdate.getPullRequestId();
        
        RepositoryPullRequestMapping pullRequest;
        pullRequest = repositoryActivityDao.findRequestById(pullRequestId, repositoryId);
        String pullRequestName = pullRequest.getPullRequestName();
        Repository repository = repositoryService.get(repositoryId);

        DvcsUser user = dvcsCommunicatorProvider.getCommunicator(repository.getDvcsType()).getUser(repository, pullRequestUpdate.getAuthor());

        Map<String, Object> templateMap = new HashMap<String, Object>();
        templateMap.put("velocity_utils", new VelocityUtils());
        templateMap.put("pullRequestUpdate", pullRequestUpdate);
        templateMap.put("pullRequestName", pullRequestName);
        templateMap.put("user", user);
        
        return new DefaultIssueAction(templateRenderer, "/templates/activity/pull-request-update-view.vm", templateMap,
                pullRequestUpdate.getLastUpdatedOn());
    }

    @Override
    public Class<? extends Object> getSupportedClass()
    {
        return RepositoryActivityPullRequestUpdateMapping.class;
    }

}
