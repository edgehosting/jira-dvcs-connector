package com.atlassian.jira.plugins.dvcs.webwork.render;

import com.atlassian.jira.plugin.issuetabpanel.IssueAction;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFile;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.util.VelocityUtils;
import com.atlassian.jira.plugins.dvcs.webwork.IssueLinker;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.templaterenderer.TemplateRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

@Component
public class ChangesetIssueActionFactory implements IssueActionFactory
{
    private final RepositoryService repositoryService;
    private final IssueLinker issueLinker;
    private final ApplicationProperties applicationProperties;
    private final ChangesetService changesetService;
    private final TemplateRenderer templateRenderer;

    @Autowired
    public ChangesetIssueActionFactory(RepositoryService repositoryService, IssueLinker issueLinker,
            @ComponentImport ApplicationProperties applicationProperties, ChangesetService changesetService,
            @ComponentImport TemplateRenderer templateRenderer)
    {
        this.repositoryService = repositoryService;
        this.issueLinker = issueLinker;
        this.applicationProperties = checkNotNull(applicationProperties);
        this.changesetService = changesetService;
        this.templateRenderer = checkNotNull(templateRenderer);
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
        templateMap.put("velocityUtils", new VelocityUtils());
        templateMap.put("changeset", changeset);

        String documentJpgUrl = applicationProperties.getBaseUrl() + "/download/resources/com.atlassian.jira.plugins.jira-bitbucket-connector-plugin/images/document.jpg";
        templateMap.put("document_jpg_url", documentJpgUrl);

        String commitUrl = changesetService.getCommitUrl(repository, changeset);

        Map<ChangesetFile, String> fileCommitUrls = changesetService.getFileCommitUrls(repository, changeset);
        templateMap.put("file_commit_urls", fileCommitUrls);

        DvcsUser user = repositoryService.getUser(repository, changeset.getAuthor(), changeset.getRawAuthor());

        templateMap.put("user", user);
        templateMap.put("changeset", changeset);
        templateMap.put("commitMessageHtml", issueLinker.createLinks(changeset.getMessage()));
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
