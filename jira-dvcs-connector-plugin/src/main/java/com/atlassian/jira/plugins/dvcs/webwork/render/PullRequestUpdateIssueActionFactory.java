package com.atlassian.jira.plugins.dvcs.webwork.render;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.atlassian.jira.plugin.issuetabpanel.IssueAction;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityCommitMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityPullRequestUpdateMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityPullRequestUpdateMapping.Status;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
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
        int repositoryId = pullRequestUpdate.getRepositoryId();
        int pullRequestId = pullRequestUpdate.getPullRequest().getID();
        
        RepositoryPullRequestMapping pullRequest = repositoryActivityDao.findRequestById(pullRequestId);
        Repository repository = repositoryService.get(repositoryId);
        
        DvcsUser user = dvcsCommunicatorProvider.getCommunicator(repository.getDvcsType()).getUser(repository, pullRequestUpdate.getAuthor());

        Map<String, Object> templateMap = new HashMap<String, Object>();
        templateMap.put("velocityUtils", new VelocityUtils());
        templateMap.put("pullRequestUpdate", pullRequestUpdate);
        templateMap.put("pullRequest", pullRequest);
        templateMap.put("repository", repository);
        templateMap.put("user", user);
        templateMap.put("lozengeStyle", getLozengeStyle(pullRequestUpdate.getStatus()));
        templateMap.put("commitUrlProvider", this);
        
        return new DefaultIssueAction(templateRenderer, "/templates/activity/pull-request-update-view.vm", templateMap,
                pullRequestUpdate.getLastUpdatedOn());
    }

    public String getCommitUrl(Repository destinationRepository, RepositoryPullRequestMapping pullRequest, RepositoryActivityCommitMapping commit)
    {
        Repository sourceRepository = getPullRequestSourceRepository(destinationRepository, pullRequest);
        
        DvcsCommunicator dvcsCommunicator = dvcsCommunicatorProvider.getCommunicator(sourceRepository.getDvcsType());
        Changeset changeset = new Changeset(0, commit.getNode(), commit.getMessage(), commit.getDate());
        return StringUtils.defaultIfEmpty(dvcsCommunicator.getCommitUrl(sourceRepository, changeset), "");
    }
    
    private Repository getPullRequestSourceRepository(Repository destinationRepository, RepositoryPullRequestMapping pullRequestMapping)
    {
        // we need to check whether we have source url, in case the fork has been deleted, we don't know the source repository
        if (pullRequestMapping.getSourceUrl() != null)
        {
            Repository repository = new Repository();
        
            repository.setOrgHostUrl(destinationRepository.getOrgHostUrl());
            repository.setDvcsType(destinationRepository.getDvcsType());
                    
            String[] split = StringUtils.split(pullRequestMapping.getSourceUrl() ,"/");
            if (split.length > 0)
            {
                repository.setOrgName(split[0]);
            }
            if (split.length > 1)
            {
                repository.setSlug(split[1]);
            }
            return repository;
        }
        return null;
    }
    
    private String getLozengeStyle(Status status)
    {
        String defaultStyle = "aui-lozenge-success aui-lozenge-subtle";
        switch (status)
        {
        case APPROVED:
            return "aui-lozenge-success aui-lozenge-subtle";
        case DECLINED:
            return "aui-lozenge-error";
        case MERGED:
            return "aui-lozenge-success";
        case OPENED:
            return "aui-lozenge-complete";
        case REOPENED:
            return defaultStyle;
        case UPDATED:
            return "aui-lozenge-current";
        default:
            return defaultStyle;
        }
    }

    @Override
    public Class<? extends Object> getSupportedClass()
    {
        return RepositoryActivityPullRequestUpdateMapping.class;
    }

}
