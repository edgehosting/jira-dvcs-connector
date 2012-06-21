package com.atlassian.jira.plugins.bitbucket.spi.github;

import org.apache.commons.lang.StringUtils;
import org.eclipse.egit.github.core.User;

import com.atlassian.jira.plugins.bitbucket.api.impl.DefaultSourceControlUser;

public class GithubUserFactory
{

    public static DefaultSourceControlUser transform(User ghUser)
    {
        final String login = ghUser.getLogin();
        final String name = ghUser.getName();

        String gravatarUrl = "https://secure.gravatar.com/avatar/" + ghUser.getGravatarId() + "?s=60";

        return new DefaultSourceControlUser(
                login,
                "",
                StringUtils.isNotBlank(name) ? name : login,  // first and last name is together in github
                gravatarUrl,
                ""
        );
    }
}
