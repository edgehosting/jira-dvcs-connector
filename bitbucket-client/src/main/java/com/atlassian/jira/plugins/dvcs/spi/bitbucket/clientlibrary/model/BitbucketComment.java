package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.util.Date;

/**
 * Class is meant to be used in all types of comment in BB in the connector.
 * 
 * <pre>
 * 
 * PR level comment like:
 * 
 *  {
 *       "is_entity_author": true,
 *       "pull_request_id": 1,
 *       "author_info": {
 *            "username": "jhocman",
 *            "first_name": "Julius",
 *            "last_name": "Hocman",
 *            "is_team": false,
 *            "avatar": "http://www.gravatar.com/avatar/23d0d498ca487a87a56289478380dd85?d=http%3A%2F%2Fcentos-6-vagrant.vagrantup.com%3A8000%2Fm%2Fdev%2Fimg%2Fdefault_avatar%2F32%2Fuser_blue.png&amp;s=32",
 *            "resource_uri": "/1.0/users/jhocman"
 *       },
 *        "pr_repo": {
 *            "owner": "jhocman",
 *            "slug": "hgrepoindahouse"
 *        },
 *        "content_rendered": "&lt;p&gt;Comment on pull request.&lt;p&gt;",
 *        "deleted": false,
 *        "utc_last_updated": "2012-12-05 09:45:36+00:00",
 *        "comment_id": 1,
 *        "filename_hash": null,
 *        "filename": null,
 *        "content": "Comment on pull request.",
 *        "parent_id": null,
 *        "convert_markup": false,
 *        "comparespec": "jhocman/hgrepoindahouse:18880990e95d..c318336af758",
 *        "line_from": null,
 *        "line_to": null,
 *        "dest_rev": null,
 *        "utc_created_on": "2012-12-05 09:45:36+00:00",
 *        "anchor": null,
 *        "is_repo_owner": true,
 *        "is_spam": false
 *    }
 *    
 *    Changeset level comment like:
 *    
 *     {
 *       "username": "tutorials",
 *       "node": "abdeaf1b2b4a",
 *       "comment_id": 25570,
 *       "display_name": "tutorials",
 *       "parent_id": null,
 *       "deleted": false,
 *       "utc_last_updated": "2012-07-23 23:17:01+00:00",
 *       "filename_hash": null,
 *       "filename": null,
 *       "content": "This is a very good change. Been waiting for it.",
 *       "content_rendered": "&lt;p&gt;This is a very good change. Been waiting for it.&lt;/p&gt;\n",
 *        "user_avatar_url": "https://secure.gravatar.com/avatar/0bc5bd490000b8e63c35c0f54e667b9e?d=identicon&amp;s=32",
 *       "line_from": null,
 *       "line_to": null,
 *       "utc_created_on": "2012-07-23 23:17:01+00:00",
 *       "is_spam": false
 *   }
 *    
 *    
 *    Used for 
 *      -   pull request comments
 *      -   for code comments in changeset in pull request
 *      -   for changeset comment
 *      -   for code comments in changeset
 *    
 *    Link construction for pull request comment:
 *    
 *    jhocman/hgrepoindahouse/pull-request/1/msg/diff#comment-1
 *    {account}/{repo}/pull-request/{prid}/msg/diff#comment-{cid}
 *    
 *    
 *    Link construction for changeset comment:
 *
 *    USUAL_CHANGESET_URL#comment-{cid}
 *     
 *    </pre>
 */
public class BitbucketComment
{

    // common comment stuff
    //
    private Integer commentId;
    private String filename; // if this is null, comment belongs to pull request, otherwise
                             // it is code comment inside pull request
    private Boolean deleted;
    private Date utcCreatedOn;
    private Date utcLastUpdated;
    private String content;

    // Changeset comments specific
    //
    private String username;
    private String displayName;
    
    // Pull Request stuff
    //
    private Integer pullRequestId;
    private BitbucketAccount authorInfo;
    private BitbucketPullRequestRepository prRepo;
    private String destRev;  // Could be the same indicator as #filename

    public BitbucketComment()
    {
        super();
    }

    public Integer getPullRequestId()
    {
        return pullRequestId;
    }

    public void setPullRequestId(Integer pullRequestId)
    {
        this.pullRequestId = pullRequestId;
    }

    public String getFilename()
    {
        return filename;
    }

    public void setFilename(String filename)
    {
        this.filename = filename;
    }

    public String getDestRev()
    {
        return destRev;
    }

    public void setDestRev(String destRev)
    {
        this.destRev = destRev;
    }

    public Boolean getDeleted()
    {
        return deleted;
    }

    public void setDeleted(Boolean deleted)
    {
        this.deleted = deleted;
    }

    public Date getUtcCreatedOn()
    {
        return utcCreatedOn;
    }

    public void setUtcCreatedOn(Date utcCreatedOn)
    {
        this.utcCreatedOn = utcCreatedOn;
    }

    public Date getUtcLastUpdated()
    {
        return utcLastUpdated;
    }

    public void setUtcLastUpdated(Date utcLastUpdated)
    {
        this.utcLastUpdated = utcLastUpdated;
    }

    public BitbucketAccount getAuthorInfo()
    {
        return authorInfo;
    }

    public void setAuthorInfo(BitbucketAccount authorInfo)
    {
        this.authorInfo = authorInfo;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public Integer getCommentId()
    {
        return commentId;
    }

    public void setCommentId(Integer commentId)
    {
        this.commentId = commentId;
    }

    public BitbucketPullRequestRepository getPrRepo()
    {
        return prRepo;
    }

    public void setPrRepo(BitbucketPullRequestRepository prRepo)
    {
        this.prRepo = prRepo;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public void setDisplayName(String displayName)
    {
        this.displayName = displayName;
    }
    
}

