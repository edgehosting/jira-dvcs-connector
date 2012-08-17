package com.atlassian.jira.plugins.dvcs.spi.github.parsers;

import org.apache.commons.lang.StringUtils;
import org.eclipse.egit.github.core.User;

import com.atlassian.jira.plugins.dvcs.model.DvcsUser;

public class GithubUserFactory
{

    public static DvcsUser transform(User ghUser)
    {
        final String login = ghUser.getLogin();
        final String name = ghUser.getName();
        String gravatarUrl = "https://secure.gravatar.com/avatar/" + ghUser.getGravatarId() + "?s=60";
        
        return new DvcsUser(
                login,
                StringUtils.isNotBlank(name) ? name : login,  // first and last name is together in github
                gravatarUrl);
    }
}
