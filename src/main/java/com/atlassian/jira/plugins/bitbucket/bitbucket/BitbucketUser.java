package com.atlassian.jira.plugins.bitbucket.bitbucket;

import com.atlassian.jira.plugins.bitbucket.bitbucket.impl.DefaultBitbucketUser;

/**
 * Describes a Bitbucket user
 */
public interface BitbucketUser
{
    static final BitbucketUser UNKNOWN_USER = new DefaultBitbucketUser(
            "unknown", "", "", "https://secure.gravatar.com/avatar/unknown?d=mm", ""
    );

    String getUsername();

    String getFirstName();

    String getLastName();

    String getAvatar();

    String getResourceUri();
}
