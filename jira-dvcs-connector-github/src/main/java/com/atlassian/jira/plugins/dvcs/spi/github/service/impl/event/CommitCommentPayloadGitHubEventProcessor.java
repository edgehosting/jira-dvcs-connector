package com.atlassian.jira.plugins.dvcs.spi.github.service.impl.event;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.lang.StringUtils;
import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.client.GitHubRequest;
import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.event.CommitCommentPayload;
import org.eclipse.egit.github.core.event.Event;
import org.springframework.beans.factory.annotation.Qualifier;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommit;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommitComment;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommitLineComment;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubRepository;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubCommitCommentService;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubCommitLineCommentService;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubCommitService;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventProcessor;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubUserService;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 * The {@link CommitCommentPayload} implementation of the {@link GitHubEventProcessor}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class CommitCommentPayloadGitHubEventProcessor extends AbstractGitHubEventProcessor<CommitCommentPayload>
{

    /**
     * @see #CommitCommentPayloadGitHubEventProcessor(GitHubCommitCommentService, GitHubCommitLineCommentService, GitHubCommitService,
     *      GitHubUserService, GithubClientProvider)
     */
    private final GitHubCommitCommentService gitHubCommitCommentService;

    /**
     * @see #CommitCommentPayloadGitHubEventProcessor(GitHubCommitCommentService, GitHubCommitLineCommentService, GitHubCommitService,
     *      GitHubUserService, GithubClientProvider)
     */
    private final GitHubCommitLineCommentService gitHubCommitLineCommentService;

    /**
     * @see #CommitCommentPayloadGitHubEventProcessor(GitHubCommitCommentService, GitHubCommitLineCommentService, GitHubCommitService,
     *      GitHubUserService, GithubClientProvider)
     */
    private final GitHubCommitService gitHubCommitService;

    /**
     * @see #CommitCommentPayloadGitHubEventProcessor(GitHubCommitCommentService, GitHubCommitLineCommentService, GitHubCommitService,
     *      GitHubUserService, GithubClientProvider)
     */
    private final GitHubUserService gitHubUserService;

    /**
     * @see #CommitCommentPayloadGitHubEventProcessor(GitHubCommitCommentService, GitHubCommitLineCommentService, GitHubCommitService,
     *      GitHubUserService, GithubClientProvider)
     */
    private final GithubClientProvider githubClientProvider;

    /**
     * Constructor.
     * 
     * @param gitHubCommitCommentService
     *            injected {@link GitHubCommitCommentService} dependency
     * @param gitHubCommitLineCommentService
     *            injected {@link GitHubCommitLineCommentService} dependency
     * @param gitHubCommitService
     *            injected {@link GitHubCommitService} dependency
     * @param gitHubUserService
     *            injected {@link GitHubUserService} dependency
     * @param githubClientProvider
     *            injected {@link GithubClientProvider} dependency
     */
    public CommitCommentPayloadGitHubEventProcessor(GitHubCommitCommentService gitHubCommitCommentService,
            GitHubCommitLineCommentService gitHubCommitLineCommentService, GitHubCommitService gitHubCommitService,
            GitHubUserService gitHubUserService, @Qualifier("githubClientProvider") GithubClientProvider githubClientProvider)
    {
        this.gitHubCommitCommentService = gitHubCommitCommentService;
        this.gitHubCommitLineCommentService = gitHubCommitLineCommentService;
        this.gitHubCommitService = gitHubCommitService;
        this.gitHubUserService = gitHubUserService;
        this.githubClientProvider = githubClientProvider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(Repository domainRepository, GitHubRepository domain, Event event)
    {
        CommitComment commitComment = getPayload(event).getComment();

        // line or general comment?
        if (StringUtils.isBlank(commitComment.getPath()))
        {
            // already proceed nothing to do
            if (gitHubCommitCommentService.getByGitHubId(commitComment.getId()) != null)
            {
                return;
            }

            //
            GitHubCommit commit = gitHubCommitService.fetch(domainRepository, domain, domain, commitComment.getCommitId());
            GitHubCommitComment gitHubCommitComment = new GitHubCommitComment();
            map(domainRepository, domain, gitHubCommitComment, commitComment, commit);
            gitHubCommitCommentService.save(gitHubCommitComment);

        } else
        {
            // already proceed nothing to do
            if (gitHubCommitLineCommentService.getByGitHubId(commitComment.getId()) != null)
            {
                return;
            }

            //
            GitHubCommit commit = gitHubCommitService.fetch(domainRepository, domain, domain, commitComment.getCommitId());
            GitHubCommitLineComment gitHubCommitLineComment = new GitHubCommitLineComment();
            map(domainRepository, domain, gitHubCommitLineComment, commitComment, commit);
            gitHubCommitLineCommentService.save(gitHubCommitLineComment);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<CommitCommentPayload> getEventPayloadType()
    {
        return CommitCommentPayload.class;
    }

    /**
     * Re-maps egit model into the internal model.
     * 
     * @param domainRepository
     *            over which repository
     * @param domain
     *            over which repository
     * @param target
     *            internal model
     * @param source
     *            egit model
     * @param commit
     *            already re-mapped {@link CommitComment#getCommitId()}
     */
    private void map(Repository domainRepository, GitHubRepository domain, GitHubCommitComment target, CommitComment source,
            GitHubCommit commit)
    {
        // TODO: workaround to get access to the HTML URL of the comment
        String htmlUrl = "";
        try
        {
            GitHubRequest request = new GitHubRequest();
            request.setUri(new URL(source.getUrl()).toURI().getRawPath());
            InputStream response = githubClientProvider.createClient(domainRepository).getStream(request);
            JsonElement element = new JsonParser().parse(new InputStreamReader(response, "UTF-8"));
            htmlUrl = element.getAsJsonObject().get("html_url").getAsString();
            response.close();

        } catch (RequestException e)
        {
            if (e.getStatus() == 404)
            {
                // silently ignored, comment was already deleted
                return;

            } else
            {
                // otherwise exception propagation
                throw new RuntimeException(e);
            }
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        } catch (URISyntaxException e)
        {
            throw new RuntimeException(e);
        }
        //

        target.setDomain(commit.getDomain());
        target.setGitHubId(source.getId());
        target.setCreatedAt(source.getCreatedAt());
        target.setCreatedBy(gitHubUserService.fetch(domainRepository, domain, source.getUser().getLogin()));
        target.setText(source.getBody());
        target.setUrl(source.getUrl());
        target.setHtmlUrl(htmlUrl);
        target.setCommit(commit);
    }

    /**
     * Re-maps egit model into the internal model.
     * 
     * @param domainRepository
     *            over which repository
     * @param domain
     *            over which repository
     * @param target
     *            internal model
     * @param source
     *            egit model
     * @param commit
     *            re-mapped {@link CommitComment#getCommitId()}
     */
    private void map(Repository domainRepository, GitHubRepository domain, GitHubCommitLineComment target, CommitComment source,
            GitHubCommit commit)
    {
        // TODO: workaround to get access to the HTML URL of the comment
        String htmlUrl = "";
        try
        {
            GitHubRequest request = new GitHubRequest();
            request.setUri(new URL(source.getUrl()).toURI().getRawPath());
            InputStream response = githubClientProvider.createClient(domainRepository).getStream(request);
            JsonElement element = new JsonParser().parse(new InputStreamReader(response, "UTF-8"));
            htmlUrl = element.getAsJsonObject().get("html_url").getAsString();
            response.close();

        } catch (RequestException e)
        {
            if (e.getStatus() == 404)
            {
                // silently ignored, comment was already deleted
                return;

            } else
            {
                // otherwise exception propagation
                throw new RuntimeException(e);
            }
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        } catch (URISyntaxException e)
        {
            throw new RuntimeException(e);
        }
        //

        target.setDomain(commit.getDomain());
        target.setGitHubId(source.getId());
        target.setCommit(commit);
        target.setUrl(source.getUrl());
        target.setHtmlUrl(htmlUrl);
        target.setCreatedAt(source.getCreatedAt());
        target.setCreatedBy(gitHubUserService.fetch(domainRepository, domain, source.getUser().getLogin()));
        target.setPath(source.getPath());
        target.setLine(source.getLine());
        target.setText(source.getBody());
    }

}
