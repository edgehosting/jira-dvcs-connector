package com.atlassian.jira.plugins.dvcs.spi.github.service.impl;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubPushDAO;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommit;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPush;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubRepository;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubPushService;

/**
 * The {@link GitHubPushService} implementation.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubPushServiceImpl implements GitHubPushService
{

    /**
     * @see #GitHubPushServiceImpl(GitHubPushDAO)
     */
    private final GitHubPushDAO gitHubPushDAO;

    /**
     * Injected {@link GitHubPushDAO} dependency.
     * 
     * @param gitHubPushDAO
     */
    public GitHubPushServiceImpl(GitHubPushDAO gitHubPushDAO)
    {
        this.gitHubPushDAO = gitHubPushDAO;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(GitHubPush gitHubPush)
    {
        gitHubPushDAO.save(gitHubPush);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(GitHubPush gitHubPush)
    {
        gitHubPushDAO.delete(gitHubPush);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubPush getById(int id)
    {
        return gitHubPushDAO.getById(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubPush getByBefore(GitHubRepository repository, String sha)
    {
        return gitHubPushDAO.getByBefore(repository, sha);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubPush getByHead(GitHubRepository repository, String sha)
    {
        return gitHubPushDAO.getByHead(repository, sha);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<GitHubPush> getByBetween(GitHubRepository repository, String fromSha, String toSha)
    {
        List<GitHubPush> result = new LinkedList<GitHubPush>();

        GitHubPush push;
        
        String cursorSha = toSha;

        untilFirstCommit: while (true)
        {
            push = getByHead(repository, cursorSha);
            cursorSha = push.getBefore();
            result.add(push);

            for (GitHubCommit commit : push.getCommits())
            {
                if (commit.getSha().equals(fromSha))
                {
                    break untilFirstCommit;
                }
            }
        }

        Collections.reverse(result);
        return result;
    }

}
