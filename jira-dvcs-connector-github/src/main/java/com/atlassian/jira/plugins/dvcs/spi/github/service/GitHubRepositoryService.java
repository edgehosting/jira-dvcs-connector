package com.atlassian.jira.plugins.dvcs.spi.github.service;

import java.util.List;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.model.review.ReviewItem;

public interface GitHubRepositoryService
{

	/**
	 * Synchronize.
	 * 
	 * @param repository
	 *            which will be synchronized
	 */
	void sync(Repository repository);

	/**
	 * Builds {@link ReviewItem}s for the provided issue.
	 * 
	 * @param issueKey
	 * @return review
	 */
	List<ReviewItem> getReview(String issueKey);

}
