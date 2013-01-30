package com.atlassian.jira.plugins.dvcs.spi.github.dao.impl;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubPullRequestDAO;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequest;

/**
 * Mock - transient implementation of the {@link GitHubPullRequestDAO}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubPullRequestDAOMockImpl implements GitHubPullRequestDAO {

	/**
	 * A {@link GitHubPullRequest#getId()} to the {@link GitHubPullRequest}.
	 */
	private final Map<Integer, GitHubPullRequest> transientStore = new ConcurrentHashMap<Integer, GitHubPullRequest>();

	/**
	 * A {@link GitHubPullRequest#getGitHubId()} to the
	 * {@link GitHubPullRequest}.
	 */
	private final Map<Long, Integer> transientStoreByGitHubId = new ConcurrentHashMap<Long, Integer>();

	/**
	 * Constructor.
	 */
	public GitHubPullRequestDAOMockImpl() {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void save(GitHubPullRequest gitHubPullRequest) {
		if (gitHubPullRequest.getId() == 0) {
			gitHubPullRequest.setId(transientStore.size() + 1);
		}

		transientStore.put(gitHubPullRequest.getId(), gitHubPullRequest);
		transientStoreByGitHubId.put(gitHubPullRequest.getGitHubId(),
				gitHubPullRequest.getId());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void delete(GitHubPullRequest gitHubPullRequest) {
		transientStore.remove(gitHubPullRequest.getId());
		transientStoreByGitHubId.remove(gitHubPullRequest.getGitHubId());
		gitHubPullRequest.setId(0);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public GitHubPullRequest getById(int id) {
		return transientStore.get(id);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public GitHubPullRequest getByGitHubId(long gitHubId) {
		Integer id = transientStoreByGitHubId.get(gitHubId);
		return id != null ? getById(id) : null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<GitHubPullRequest> getGitHubPullRequest(String issueKey) {
		return new LinkedList<GitHubPullRequest>(transientStore.values());
	}

}
