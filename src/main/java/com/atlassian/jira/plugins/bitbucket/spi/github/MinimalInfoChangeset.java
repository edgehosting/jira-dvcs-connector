package com.atlassian.jira.plugins.bitbucket.spi.github;

import com.atlassian.jira.plugins.bitbucket.api.ChangesetFile;
import com.atlassian.jira.plugins.bitbucket.spi.DefaultBitbucketChangeset;

import java.util.Collections;
import java.util.Date;

public class MinimalInfoChangeset extends DefaultBitbucketChangeset
{
    public MinimalInfoChangeset(int repositoryId, String node, String message)
    {
        super(repositoryId, node, "","",new Date(),"","",message, Collections.<String>emptyList(), Collections.<ChangesetFile>emptyList(), 0);
    }
}
