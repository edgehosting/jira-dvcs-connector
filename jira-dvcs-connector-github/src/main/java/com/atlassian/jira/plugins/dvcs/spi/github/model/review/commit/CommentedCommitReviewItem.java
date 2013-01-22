package com.atlassian.jira.plugins.dvcs.spi.github.model.review.commit;

import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommit;
import com.atlassian.jira.plugins.dvcs.spi.github.model.review.ReviewItem;

/**
 * A comment assigned to the {@link GitHubCommit}.
 * 
 * @author stanislav-dvorscak@solumiss.eu
 * 
 */
public class CommentedCommitReviewItem extends ReviewItem {

	/**
	 * @see #getCommit()
	 */
	private GitHubCommit commit;

	/**
	 * @see #getComment()
	 */
	private String comment;

	/**
	 * Constructor.
	 */
	public CommentedCommitReviewItem() {
	}

	/**
	 * @return A commented commit.
	 */
	public GitHubCommit getCommit() {
		return commit;
	}

	/**
	 * @param commit
	 *            {@link #getCommit()}
	 */
	public void setCommit(GitHubCommit commit) {
		this.commit = commit;
	}

	/**
	 * @return A comment assigned to the commit.
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * @param comment
	 *            {@link #getComment()}
	 */
	public void setComment(String comment) {
		this.comment = comment;
	}

}
