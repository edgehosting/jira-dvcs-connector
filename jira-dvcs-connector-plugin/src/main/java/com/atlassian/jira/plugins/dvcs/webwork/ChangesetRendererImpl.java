package com.atlassian.jira.plugins.dvcs.webwork;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugin.issuetabpanel.IssueAction;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFile;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.util.VelocityUtils;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

@Component
public class ChangesetRendererImpl implements ChangesetRenderer
{

    private final Logger logger = LoggerFactory.getLogger(ChangesetRendererImpl.class);

    private final ChangesetService changesetService;
    private final RepositoryService repositoryService;
    private final IssueLinker issueLinker;
    private final ApplicationProperties applicationProperties;

    private final TemplateRenderer templateRenderer;
    private final IssueAndProjectKeyManager issueAndProjectKeyManager;

    @Autowired
    public ChangesetRendererImpl(ChangesetService changesetService, RepositoryService repositoryService, IssueLinker issueLinker,
            @ComponentImport ApplicationProperties applicationProperties, @ComponentImport TemplateRenderer templateRenderer,
            IssueAndProjectKeyManager issueAndProjectKeyManager)
    {
        this.changesetService = changesetService;
        this.repositoryService = repositoryService;
        this.issueLinker = issueLinker;
        this.applicationProperties = applicationProperties;
        this.templateRenderer = checkNotNull(templateRenderer);
        this.issueAndProjectKeyManager = issueAndProjectKeyManager;
    }

    @Override
    public List<IssueAction> getAsActions(Issue issue)
    {
        Set<String> issueKeys = issueAndProjectKeyManager.getAllIssueKeys(issue);
        return getAsActions(issueKeys);
    }

    private List<IssueAction> getAsActions(Set<String> issueKeys)
    {
        List<IssueAction> bitbucketActions = new ArrayList<IssueAction>();
        try
        {
            final List<Changeset> changesets = changesetService.getByIssueKey(issueKeys, false);
            final List<Changeset> changesetDetails = changesetService.getChangesetsWithFileDetails(changesets);

            for (Changeset changeset : changesetDetails)
            {
                logger.debug("found changeset [ {} ] on issue keys [ {} ]", changeset.getNode(), StringUtils.join(issueKeys, ", "));

                String changesetAsHtml = getHtmlForChangeset(changeset);
                if (StringUtils.isNotBlank(changesetAsHtml))
                {
                    bitbucketActions.add(new CommitsIssueAction(changesetAsHtml, changeset.getDate()));
                }
            }
        }
        catch (SourceControlException e)
        {
            logger.debug("Could not retrieve changeset for [ " + StringUtils.join(issueKeys, ", ") + " ]: " + e, e);
        }

        return bitbucketActions;
    }

    public String getHtmlForChangeset(Changeset changeset)
    {
        Map<String, Object> templateMap = new HashMap<String, Object>();

        templateMap.put("velocity_utils", new VelocityUtils());
        templateMap.put("issue_linker", issueLinker);

        String documentJpgUrl = applicationProperties.getBaseUrl()
                + "/download/resources/com.atlassian.jira.plugins.jira-bitbucket-connector-plugin/images/document.jpg";
        templateMap.put("document_jpg_url", documentJpgUrl);

        List<Repository> repositories = Lists.newArrayList();

        Map<Repository, String> commitUrlsByRepo = Maps.newHashMap();
        Map<Repository, Map<ChangesetFile, String>> fileCommitUrlsByRepo = Maps.newHashMap();

        Repository firstRepository = null;

        for (Integer repositoryId : changeset.getRepositoryIds())
        {
            Repository repository = repositoryService.get(repositoryId);
            if (repository == null || repository.isDeleted() || !repository.isLinked())
            {
                continue;
            }

            if (repositoryId == changeset.getRepositoryId())
            {
                firstRepository = repository;
                repositories.add(0, repository);
            }
            else
            {
                repositories.add(repository);
            }

            String commitUrl = changesetService.getCommitUrl(repository, changeset);
            commitUrlsByRepo.put(repository, commitUrl);

            Map<ChangesetFile, String> fileCommitUrls = changesetService.getFileCommitUrls(repository, changeset);
            fileCommitUrlsByRepo.put(repository, fileCommitUrls);
        }

        // all repositories which are associated with given changesets is
        // deleted or unlinked
        if (repositories.isEmpty())
        {
            return null;
        }

        String authorName = changeset.getRawAuthor();
        String login = changeset.getAuthor();

        templateMap.put("changeset", changeset);
        templateMap.put("repositories", repositories);
        templateMap.put("commit_urls_by_repo", commitUrlsByRepo);
        templateMap.put("file_commit_urls_by_repo", fileCommitUrlsByRepo);

        DvcsUser user = repositoryService.getUser(firstRepository, changeset.getAuthor(), changeset.getRawAuthor());

        String commitMessage = changeset.getMessage();

        templateMap.put("gravatar_url", user.getAvatar());
        templateMap.put("user_url", user.getUrl());
        templateMap.put("login", login);
        templateMap.put("user_name", authorName);
        templateMap.put("commit_message", commitMessage);
        templateMap.put("commit_hash", changeset.getNode());
        templateMap.put("max_visible_files", Changeset.MAX_VISIBLE_FILES);

        StringWriter sw = new StringWriter();
        try
        {
            templateRenderer.render("/templates/commits-view.vm", templateMap, sw);
        }
        catch (IOException e)
        {
            logger.warn(e.getMessage(), e);
        }
        return sw.toString();
    }
}
