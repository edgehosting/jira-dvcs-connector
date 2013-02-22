package com.atlassian.jira.plugins.dvcs.webwork;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFile;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.util.VelocityUtils;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.templaterenderer.TemplateRenderer;

public class ChangesetRendererImpl implements ChangesetRenderer {

    private final Logger logger = LoggerFactory.getLogger(ChangesetRendererImpl.class);

    private final ChangesetService changesetService;
    private final RepositoryService repositoryService;
    private final IssueLinker issueLinker;
    private final ApplicationProperties applicationProperties;

    private final TemplateRenderer templateRenderer;

    public ChangesetRendererImpl(ChangesetService changesetService, RepositoryService repositoryService, IssueLinker issueLinker, ApplicationProperties applicationProperties, TemplateRenderer templateRenderer) {
        this.changesetService = changesetService;
        this.repositoryService = repositoryService;
        this.issueLinker = issueLinker;
        this.applicationProperties = applicationProperties;
        this.templateRenderer = templateRenderer;
    }

    @Override
    public String getHtmlForChangeset(Changeset changeset)
    {
        Repository repository = repositoryService.get(changeset.getRepositoryId());
        if (repository == null || repository.isDeleted() || !repository.isLinked())
        {
            return "";
        }

        Map<String, Object> templateMap = getVelocityContextForChangeset(changeset, repository);

        StringWriter sw = new StringWriter();
        try
        {
            templateRenderer.render("/templates/commits-view.vm", templateMap, sw);
        } catch (IOException e)
        {
            logger.warn(e.getMessage(), e);
        }
        return sw.toString();
    }

    @Override
    public Map<String, Object> getVelocityContextForChangeset(Changeset changeset, Repository repository)
    {
        Map<String, Object> templateMap = new HashMap<String, Object>();
        templateMap.put("velocity_utils", new VelocityUtils());
        templateMap.put("issue_linker", issueLinker);
        templateMap.put("changeset", changeset);

        String documentJpgUrl = applicationProperties.getBaseUrl() + "/download/resources/com.atlassian.jira.plugins.jira-bitbucket-connector-plugin/images/document.jpg";
        templateMap.put("document_jpg_url", documentJpgUrl);

        String authorName = changeset.getRawAuthor();
        String login = changeset.getAuthor();

        String commitUrl = changesetService.getCommitUrl(repository, changeset);

        Map<ChangesetFile, String> fileCommitUrls = changesetService.getFileCommitUrls(repository, changeset);
        templateMap.put("file_commit_urls", fileCommitUrls);

        DvcsUser user = repositoryService.getUser(repository, changeset.getAuthor(), changeset.getRawAuthor());

        String commitMessage = changeset.getMessage();

        templateMap.put("gravatar_url", user.getAvatar());
        templateMap.put("user_url", user.getUrl());
        templateMap.put("login", login);
        templateMap.put("user_name", authorName);
        templateMap.put("commit_message", commitMessage);
        templateMap.put("commit_url", commitUrl);
        templateMap.put("commit_hash", changeset.getNode());
        templateMap.put("max_visible_files", Changeset.MAX_VISIBLE_FILES);
        return templateMap;
    }
}
