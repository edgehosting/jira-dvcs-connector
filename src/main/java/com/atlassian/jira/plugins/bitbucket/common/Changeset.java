package com.atlassian.jira.plugins.bitbucket.common;

import java.util.List;

import com.atlassian.jira.plugins.bitbucket.bitbucket.BitbucketChangesetFile;

/**
 * Details on a changeset
 */
public interface Changeset
{
	
	public String getRepositoryUrl();
	
    public String getNode();

    public String getRawAuthor();

    public String getAuthor();

    public String getTimestamp();

    public String getRawNode();

    public String getBranch();

    public String getMessage();

    public List<String> getParents();

    public List<BitbucketChangesetFile> getFiles();

    public String getRevision();

    public String getCommitURL();
}
