package com.atlassian.jira.plugins.dvcs.webwork.render;

import java.util.HashMap;
import java.util.Map;

import com.atlassian.jira.plugin.issuetabpanel.IssueAction;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryCommitCommentActivityMapping;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.util.VelocityUtils;
import com.atlassian.templaterenderer.TemplateRenderer;

public class CommitCommentIssueActionFactory implements IssueActionFactory
{
    private final RepositoryService repositoryService;
    private final TemplateRenderer templateRenderer;

    public CommitCommentIssueActionFactory(RepositoryService repositoryService, TemplateRenderer templateRenderer)
    {
        this.repositoryService = repositoryService;
        this.templateRenderer = templateRenderer;
    }

    @Override
    public IssueAction create(Object activityItem)
    {
        RepositoryCommitCommentActivityMapping commitComment = (RepositoryCommitCommentActivityMapping) activityItem;
        Repository repository = repositoryService.get(commitComment.getRepositoryId());
        DvcsUser user = repositoryService.getUser(repository, commitComment.getAuthor(), commitComment.getRawAuthor());

        Map<String, Object> templateMap = new HashMap<String, Object>();
        templateMap.put("velocityUtils", new VelocityUtils());
        templateMap.put("commitComment", commitComment);
        templateMap.put("repository", repository);
        templateMap.put("user", user);

        return new DefaultIssueAction(templateRenderer, "/templates/activity/commit-comment-view.vm", templateMap,
                commitComment.getLastUpdatedOn());
    }

    @Override
    public Class<? extends Object> getSupportedClass()
    {
        return RepositoryCommitCommentActivityMapping.class;
    }

}
