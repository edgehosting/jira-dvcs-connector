package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;
import java.util.List;

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
    
    private BitbucketPullRequestCommentActivityContent content;

    public BitbucketPullRequestCommentActivity()
    {
        super();
    }

    public BitbucketPullRequestCommentActivityContent getContent()
    {
        return content;
    }

    public void setContent(BitbucketPullRequestCommentActivityContent content)
    {
        this.content = content;
    }
    
    @Override
    public List<String> getMessages()
    {
        List<String> messages = super.getMessages();
        if (content != null)
        {
            messages.add(content.getRaw());
        }
        return messages;
    }

}

