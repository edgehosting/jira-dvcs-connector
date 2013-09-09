package com.atlassian.jira.plugins.dvcs.webwork.render;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.atlassian.jira.plugin.issuetabpanel.IssueAction;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryCommitMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestUpdateActivityMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestUpdateActivityMapping.Status;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.util.VelocityUtils;
import com.atlassian.templaterenderer.TemplateRenderer;

public class PullRequestUpdateIssueActionFactory implements IssueActionFactory
{
    private final RepositoryService repositoryService;
    private final TemplateRenderer templateRenderer;
    private final RepositoryActivityDao repositoryActivityDao;
    private final ChangesetService changesetService;

    public PullRequestUpdateIssueActionFactory(RepositoryService repositoryService, TemplateRenderer templateRenderer,
            RepositoryActivityDao repositoryActivityDao, ChangesetService changesetService)
    {
        this.repositoryService = repositoryService;
        this.templateRenderer = templateRenderer;
        this.repositoryActivityDao = repositoryActivityDao;
        this.changesetService = changesetService;
    }

    @Override
    public IssueAction create(Object activityItem)
    {
        RepositoryPullRequestUpdateActivityMapping pullRequestUpdate = (RepositoryPullRequestUpdateActivityMapping) activityItem;
        int repositoryId = pullRequestUpdate.getRepositoryId();
        int pullRequestId = pullRequestUpdate.getPullRequest().getID();

        RepositoryPullRequestMapping pullRequest = repositoryActivityDao.findRequestById(pullRequestId);
        Repository repository = repositoryService.get(repositoryId);

        Map<String, Object> view = new HashMap<String, Object>();
        
        // defaults
        view.put("velocityUtils", new VelocityUtils());
        
        // views
        getView(view, pullRequest);
        getView(view, repository, pullRequest, pullRequestUpdate);
        
        // action
        return new DefaultIssueAction(templateRenderer, "/templates/activity/pull-request-update-view.vm", view, pullRequestUpdate.getLastUpdatedOn());
    }
    
    private void getView(Map<String, Object> target, RepositoryPullRequestMapping pullRequest) {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("name", pullRequest.getName());
        result.put("url", pullRequest.getUrl());

        target.put("pullRequest", result);
    }

    private void getView(Map<String, Object> target, Repository repository, RepositoryPullRequestMapping pullRequest,
            RepositoryPullRequestUpdateActivityMapping pullRequestUpdate)
    {
        DvcsUser author = repositoryService.getUser(repository, pullRequestUpdate.getAuthor(), null);
        
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("status", pullRequestUpdate.getStatus());
        result.put("lozengeStyle", getLozengeStyle(pullRequestUpdate.getStatus()));
        result.put("authorName", author.getFullName());
        result.put("authorUrl", StringUtils.isEmpty(author.getUrl()) ? "#" : author.getUrl());
        result.put("authorAvatar", author.getAvatar());
        
        // commits
        List<Map<String, Object>> commits = new LinkedList<Map<String, Object>>();
        for (RepositoryCommitMapping commit : pullRequestUpdate.getCommits())
        {
            commits.add(getView(repository, pullRequest, pullRequestUpdate, commit));
        }
        result.put("commits", commits);
    
        target.put("pullRequestUpdate", result);
    }

    private Map<String, Object> getView(Repository repository, RepositoryPullRequestMapping pullRequest,
            RepositoryPullRequestUpdateActivityMapping pullRequestUpdate, RepositoryCommitMapping commit)
    {
        DvcsUser author = repositoryService.getUser(repository, commit.getAuthor(), null);

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("authorName", author.getFullName());
        result.put("authorAvatarUrl", author.getAvatar());
        result.put("authorUrl", StringUtils.isEmpty(author.getUrl()) ? "#" : author.getUrl());
        result.put("url", getCommitUrl(repository, pullRequest, commit));
        result.put("node", commit.getNode().substring(0, 7));
        result.put("date", commit.getDate());
        result.put("message", commit.getMessage());
        return result;
    }

    public String getCommitUrl(Repository destinationRepository, RepositoryPullRequestMapping pullRequest,
            RepositoryCommitMapping commit)
    {
        Repository sourceRepository = createMockRepository(pullRequest.getSourceUrl(), destinationRepository.getDvcsType());
        if (sourceRepository == null)
        {
            return "";
        }
        Changeset changeset = new Changeset(0, commit.getNode(), commit.getMessage(), commit.getDate());
        return changesetService.getCommitUrl(sourceRepository, changeset);
    }

    private static Repository createMockRepository(String repositoryUrl, String dvcsType)
    {
        if (StringUtils.isBlank(repositoryUrl))
        {
            return null;
        }
        Pattern pattern = Pattern.compile("(.*)/(.*)/(.*)");
        Matcher matcher = pattern.matcher(repositoryUrl);
        if (matcher.find())
        {
            Repository repository = new Repository();
            repository.setDvcsType(dvcsType);
            repository.setRepositoryUrl(repositoryUrl);
            repository.setOrgHostUrl(matcher.group(1));
            repository.setOrgName(matcher.group(2));
            repository.setSlug(matcher.group(3));
            repository.setName(matcher.group(3));
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
        return RepositoryPullRequestUpdateActivityMapping.class;
    }

}
