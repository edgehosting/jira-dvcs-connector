package com.atlassian.jira.plugins.dvcs.webwork.render;

import java.util.HashMap;
import java.util.Map;

import com.atlassian.jira.plugin.issuetabpanel.IssueAction;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityPullRequestCommentMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityPullRequestLineCommentMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.atlassian.jira.plugins.dvcs.util.VelocityUtils;
import com.atlassian.templaterenderer.TemplateRenderer;

public class PullRequestLineCommentIssueActionFactory implements IssueActionFactory
{
    private final RepositoryService repositoryService;
    private final TemplateRenderer templateRenderer;
    private final DvcsCommunicatorProvider dvcsCommunicatorProvider;

    public PullRequestLineCommentIssueActionFactory(RepositoryService repositoryService, 
            TemplateRenderer templateRenderer, 
            DvcsCommunicatorProvider dvcsCommunicatorProvider)
    {
        this.repositoryService = repositoryService;
        this.templateRenderer = templateRenderer;
        this.dvcsCommunicatorProvider = dvcsCommunicatorProvider;
    }

    @Override
    public IssueAction create(Object activityItem)
    {
        RepositoryActivityPullRequestLineCommentMapping pullRequestLineComment = (RepositoryActivityPullRequestLineCommentMapping) activityItem;
        int repositoryId = pullRequestLineComment.getRepositoryId();
        RepositoryPullRequestMapping pullRequest = pullRequestLineComment.getPullRequest();
        
        Repository repository = repositoryService.get(repositoryId);

        DvcsUser user = dvcsCommunicatorProvider.getCommunicator(repository.getDvcsType()).getUser(repository, pullRequestLineComment.getAuthor());

        Map<String, Object> templateMap = new HashMap<String, Object>();
        templateMap.put("velocityUtils", new VelocityUtils());
        templateMap.put("pullRequestComment", pullRequestLineComment);
        templateMap.put("pullRequest", pullRequest);
        templateMap.put("user", user);
        
        return new DefaultIssueAction(templateRenderer, "/templates/activity/pull-request-line-comment-view.vm", templateMap,
                pullRequestLineComment.getLastUpdatedOn());
    }

    @Override
    public Class<? extends Object> getSupportedClass()
    {
        return RepositoryActivityPullRequestCommentMapping.class;
    }

}
