package com.atlassian.jira.plugins.dvcs.webwork.render;

import java.util.HashMap;
import java.util.Map;

import com.atlassian.jira.plugin.issuetabpanel.IssueAction;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityPullRequestCommentMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.util.VelocityUtils;
import com.atlassian.templaterenderer.TemplateRenderer;

public class PullRequestCommentIssueActionFactory implements IssueActionFactory
{
    private final RepositoryService repositoryService;
    private final TemplateRenderer templateRenderer;

    public PullRequestCommentIssueActionFactory(RepositoryService repositoryService, 
            TemplateRenderer templateRenderer)
    {
        this.repositoryService = repositoryService;
        this.templateRenderer = templateRenderer;
    }

    @Override
    public IssueAction create(Object activityItem)
    {
        RepositoryActivityPullRequestCommentMapping pullRequestComment = (RepositoryActivityPullRequestCommentMapping) activityItem;
        int repositoryId = pullRequestComment.getRepositoryId();
        RepositoryPullRequestMapping pullRequest = pullRequestComment.getPullRequest();
        
        for (Repository repository : repositoryService.getAllRepositories()) {
            System.out.println(repository.getId() + ":" + repository.getName());
        }
        Repository repository = repositoryService.get(repositoryId);
        DvcsUser user = repositoryService.getUser(repository, pullRequestComment.getAuthor(), pullRequestComment.getRawAuthor());

        Map<String, Object> templateMap = new HashMap<String, Object>();
        templateMap.put("velocityUtils", new VelocityUtils());
        templateMap.put("pullRequestComment", pullRequestComment);
        templateMap.put("pullRequest", pullRequest);
        templateMap.put("repository", repository);
        templateMap.put("user", user);
        
        return new DefaultIssueAction(templateRenderer, "/templates/activity/pull-request-comment-view.vm", templateMap,
                pullRequestComment.getLastUpdatedOn());
    }

    @Override
    public Class<? extends Object> getSupportedClass()
    {
        return RepositoryActivityPullRequestCommentMapping.class;
    }

}
