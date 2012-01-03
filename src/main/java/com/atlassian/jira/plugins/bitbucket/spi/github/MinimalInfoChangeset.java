package com.atlassian.jira.plugins.bitbucket.spi.github;

import com.atlassian.jira.plugins.bitbucket.api.ChangesetFile;
import com.atlassian.jira.plugins.bitbucket.spi.DefaultChangeset;

import java.util.Collections;
import java.util.Date;

public class MinimalInfoChangeset extends DefaultChangeset
{
    // TODO check why is this in its own class
    public MinimalInfoChangeset(int repositoryId, String node, String message)
    {
        super(repositoryId, node, "","",new Date(),"","",message, Collections.<String>emptyList(), Collections.<ChangesetFile>emptyList(), 0);
    }
}
