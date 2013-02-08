package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;

/**
 *
 * @author mstencel@atlassian.com
 */
public class BitbucketPullRequestUpdateActivitySource extends BitbucketPullRequestBaseActivity implements Serializable, HasPossibleUpdatedMessages
{
	private static final long serialVersionUID = -5134849227580638384L;

	private BitbucketPullRequestCommit commit;

	public BitbucketPullRequestCommit getCommit() {
		return commit;
	}

	public void setCommit(BitbucketPullRequestCommit commit) {
		this.commit = commit;
	}
}