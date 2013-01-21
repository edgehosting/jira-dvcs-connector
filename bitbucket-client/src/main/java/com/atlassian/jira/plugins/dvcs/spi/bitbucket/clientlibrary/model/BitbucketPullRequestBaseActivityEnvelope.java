package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;

/**
 * 
 * ...
 * [
 * {
 * "item": {
 *  "pr": {"id": 428, "link": {"rel": "self", "href": "/1.0/..."}},
 *  "comment": {...}
 * }
 * ... 
 * 
 * <br /><br />
 * Created on 15.1.2013, 15:43:14
 * <br /><br />
 * @author jhocman@atlassian.com
 *
 */
/**
 * BitbucketPullRequestBaseActivityEnvelope
 *
 * 
 * <br /><br />
 * Created on 15.1.2013, 15:48:41
 * <br /><br />
 * @author jhocman@atlassian.com
 *
 */
public class BitbucketPullRequestBaseActivityEnvelope implements Serializable
{
    private static final long serialVersionUID = -4076516797342633690L;
    
    private BitbucketPullRequestActivityInfo item;
    
    public BitbucketPullRequestBaseActivityEnvelope()
    {
        super();
    }

    public BitbucketPullRequestActivityInfo getItem()
    {
        return item;
    }

    public void setItem(BitbucketPullRequestActivityInfo item)
    {
        this.item = item;
    }

   
    

}

