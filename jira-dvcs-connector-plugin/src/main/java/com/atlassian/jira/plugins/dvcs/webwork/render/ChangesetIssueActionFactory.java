package com.atlassian.jira.plugins.dvcs.webwork.render;

import java.util.HashMap;
import java.util.Map;

import com.atlassian.jira.plugin.issuetabpanel.IssueAction;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFile;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.util.VelocityUtils;
import com.atlassian.jira.plugins.dvcs.webwork.IssueLinker;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.templaterenderer.TemplateRenderer;

public class ChangesetIssueActionFactory implements IssueActionFactory
{
    private final RepositoryService repositoryService;
    private final IssueLinker issueLinker;
    private final ApplicationProperties applicationProperties;
    private final ChangesetService changesetService;
    private final TemplateRenderer templateRenderer;

    public ChangesetIssueActionFactory(RepositoryService repositoryService, IssueLinker issueLinker,
            ApplicationProperties applicationProperties, ChangesetService changesetService,
            TemplateRenderer templateRenderer)
    {
        this.repositoryService = repositoryService;
        this.issueLinker = issueLinker;
        this.applicationProperties = applicationProperties;
        this.changesetService = changesetService;
        this.templateRenderer = templateRenderer;
    }

    @Override
    public IssueAction create(Object activityItem)
    {
        Changeset changeset = (Changeset) activityItem;
        
        Repository repository = repositoryService.get(changeset.getRepositoryId());
        if (repository == null || repository.isDeleted() || !repository.isLinked())
        {
            return null;  // TODO maybe return some empty GenericIssueAction?
        }

        Map<String, Object> templateMap = new HashMap<String, Object>();
        templateMap.put("velocity_utils", new VelocityUtils());
        templateMap.put("changeset", changeset);

        String documentJpgUrl = applicationProperties.getBaseUrl() + "/download/resources/com.atlassian.jira.plugins.jira-bitbucket-connector-plugin/images/document.jpg";
        templateMap.put("document_jpg_url", documentJpgUrl);

        String login = changeset.getAuthor();
        String commitUrl = changesetService.getCommitUrl(repository, changeset);

        Map<ChangesetFile, String> fileCommitUrls = changesetService.getFileCommitUrls(repository, changeset);
        templateMap.put("file_commit_urls", fileCommitUrls);

        DvcsUser user = changesetService.getUser(repository, changeset);

        String gravatarUrl = user.getAvatar().replace("s=32", "s=60");
        String commitMessage = changeset.getMessage();

        templateMap.put("gravatar_url", gravatarUrl);

        String userUrl = changesetService.getUserUrl(repository, changeset);
        templateMap.put("user_url", userUrl);

        templateMap.put("login", login);
        templateMap.put("user", user);
        templateMap.put("changeset", changeset);
        templateMap.put("commitMessage", commitMessage);
        templateMap.put("commitMessageHtml", issueLinker.createLinks(commitMessage));
        templateMap.put("commit_url", commitUrl);
        templateMap.put("commit_hash", changeset.getNode());
        
        return new DefaultIssueAction(templateRenderer, "/templates/activity/commit-view.vm", templateMap,
                changeset.getDate());
    }

    @Override
    public Class<? extends Object> getSupportedClass()
    {
        return Changeset.class;
    }

}