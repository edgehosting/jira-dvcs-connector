package com.atlassian.jira.plugins.dvcs.spi.github.service.impl.event;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.event.CommitCommentPayload;
import org.eclipse.egit.github.core.event.Event;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommit;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommitComment;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommitLineComment;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubCommitCommentService;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubCommitLineCommentService;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubCommitService;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventProcessor;

/**
 * The {@link CommitCommentPayload} implementation of the {@link GitHubEventProcessor}.
 * 
 * @author stanislav-dvorscak@solumiss.eu
 * 
 */
public class CommitCommentPayloadGitHubEventProcessor extends AbstractGitHubEventProcessor<CommitCommentPayload>
{

    /**
     * @see #CommitCommentPayloadGitHubEventProcessor(GitHubCommitCommentService, GitHubCommitLineCommentService, GitHubCommitService)
     */
    private final GitHubCommitCommentService gitHubCommitCommentService;

    /**
     * @see #CommitCommentPayloadGitHubEventProcessor(GitHubCommitCommentService, GitHubCommitLineCommentService, GitHubCommitService)
     */
    private final GitHubCommitLineCommentService gitHubCommitLineCommentService;

    /**
     * @see #CommitCommentPayloadGitHubEventProcessor(GitHubCommitCommentService, GitHubCommitLineCommentService, GitHubCommitService)
     */
    private final GitHubCommitService gitHubCommitService;

    /**
     * Constructor.
     * 
     * @param gitHubCommitCommentService
     *            Injected {@link GitHubCommitCommentService} dependency.
     * @param gitHubCommitLineCommentService
     *            Injected {@link GitHubCommitLineCommentService} dependency.
     * @param gitHubCommitService
     *            Injected {@link GitHubCommitService} dependency.
     */
    public CommitCommentPayloadGitHubEventProcessor(GitHubCommitCommentService gitHubCommitCommentService,
            GitHubCommitLineCommentService gitHubCommitLineCommentService, GitHubCommitService gitHubCommitService)
    {
        this.gitHubCommitCommentService = gitHubCommitCommentService;
        this.gitHubCommitLineCommentService = gitHubCommitLineCommentService;
        this.gitHubCommitService = gitHubCommitService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(Repository repository, Event event)
    {
        CommitComment commitComment = getPayload(event).getComment();

        // line or general comment?
        if (commitComment.getPath() == null || commitComment.getPath().trim().isEmpty())
        {
            // already proceed nothing to do
            if (gitHubCommitCommentService.getByGitHubId(commitComment.getId()) != null)
            {
                return;
            }

            //
            GitHubCommit commit = gitHubCommitService.getBySha(commitComment.getCommitId());
            GitHubCommitComment gitHubCommitComment = new GitHubCommitComment();
            gitHubCommitCommentService.map(gitHubCommitComment, commitComment, commit);
            gitHubCommitCommentService.save(gitHubCommitComment);

        } else
        {
            // already proceed nothing to do
            if (gitHubCommitLineCommentService.getByGitHubId(commitComment.getId()) != null)
            {
                return;
            }

            //
            GitHubCommit commit = gitHubCommitService.getBySha(commitComment.getCommitId());
            GitHubCommitLineComment gitHubCommitLineComment = new GitHubCommitLineComment();
            gitHubCommitLineCommentService.map(gitHubCommitLineComment, commitComment, commit);
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

}
