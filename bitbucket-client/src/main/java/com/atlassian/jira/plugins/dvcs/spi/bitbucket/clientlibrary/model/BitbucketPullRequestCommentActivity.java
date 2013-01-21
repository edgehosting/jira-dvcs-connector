package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;

/**
 * BitbucketPullRequest
 *
 * 
 * <br /><br />
 * Created on 11.12.2012, 14:02:57
 * <br /><br />
 * @author jhocman@atlassian.com
 *
 */
public class BitbucketPullRequestCommentActivity extends BitbucketPullRequestBaseActivity implements Serializable
{

    private static final long serialVersionUID = 8212352604704981087L;
    
    private String cm;

    public BitbucketPullRequestCommentActivity()
    {
        super();
    }

    @Override
    public Iterable<String> getMessages()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String getCm()
    {
        return cm;
    }

    public void setCm(String cm)
    {
        this.cm = cm;
    }
    
    


}

