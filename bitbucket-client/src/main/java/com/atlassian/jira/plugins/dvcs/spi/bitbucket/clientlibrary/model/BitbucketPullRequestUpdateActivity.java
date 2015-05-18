package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;

/**
 * TODO commit on update ?? not array ?
 * 
 *          "source": {
                    "commit": {
                        "sha": "4a233e7b8596",
                        "href": "/api/1.0/repositories/erik/bitbucket/changesets/4a233e7b8596"
                    },
                    "branch": {
                        "name": "erik/captcha"
                    },
                    "repository": {
                        "href": "/api/1.0/repositories/erik/bitbucket"
                    }
                },
 * 
 * @author jhocman@atlassian.com
 */
public class BitbucketPullRequestUpdateActivity extends BitbucketPullRequestBaseActivity implements Serializable
{
    private static final long serialVersionUID = -7371697488007134175L;
    
    private String title;
    
    private String description;

    private String state;

    private BitbucketPullRequestHead source;
    
    public BitbucketPullRequestUpdateActivity()
    {
        super();
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getState()
    {
        return state;
    }

    public void setState(String state)
    {
        this.state = state;
    }

    public BitbucketPullRequestHead getSource()
    {
        return source;
    }

    public void setSource(BitbucketPullRequestHead source)
    {
        this.source = source;
    }
}

