package com.atlassian.jira.plugins.dvcs.spi.github.parsers;

import org.apache.commons.lang.StringUtils;
import org.eclipse.egit.github.core.User;

import com.atlassian.jira.plugins.dvcs.model.DvcsUser;

public class GithubUserFactory
{

    public static DvcsUser transform(User ghUser, String hostUrl)
    {
        String login = ghUser.getLogin();
        String name = ghUser.getName();
        String gravatarUrl = ghUser.getAvatarUrl();
        
        return new DvcsUser(
                login,
                StringUtils.isNotBlank(name) ? name : login,  // first and last name is together in github
                gravatarUrl, hostUrl);
    }
}
