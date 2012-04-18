package com.atlassian.jira.plugins.bitbucket.api;

import com.atlassian.jira.plugins.bitbucket.api.impl.DefaultSourceControlUser;

/**
 * Describes a Source Control user
 */
public interface SourceControlUser
{
    static final SourceControlUser UNKNOWN_USER = new DefaultSourceControlUser(
            "unknown", "", "", "https://secure.gravatar.com/avatar/unknown?d=mm", ""
    );

    String getUsername();

    String getFirstName();

    String getLastName();

    String getAvatar();

    String getResourceUri();

}
