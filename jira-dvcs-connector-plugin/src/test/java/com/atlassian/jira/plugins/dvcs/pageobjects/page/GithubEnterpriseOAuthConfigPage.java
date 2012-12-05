package com.atlassian.jira.plugins.dvcs.pageobjects.page;


/**
 *
 */
public class GithubEnterpriseOAuthConfigPage extends GithubOAuthConfigPage
{
    @Override
    public String getUrl()
    {
        return "/secure/admin/ConfigureGithubEnterpriseOAuth!default.jspa";
    }
}
