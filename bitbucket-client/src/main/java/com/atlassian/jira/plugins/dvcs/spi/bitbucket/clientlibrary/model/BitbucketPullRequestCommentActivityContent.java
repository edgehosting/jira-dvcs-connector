package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;

public class BitbucketPullRequestCommentActivityContent implements Serializable
{

    private static final long serialVersionUID = 8212352604704981087L;
    
    private String raw;

    public BitbucketPullRequestCommentActivityContent()
    {
        super();
    }

    public String getRaw()
    {
        return raw;
    }

    public void setRaw(String raw)
    {
        this.raw = raw;
    }


}

