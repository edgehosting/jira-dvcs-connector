package it.restart.com.atlassian.jira.plugins.dvcs.github;

import it.restart.com.atlassian.jira.plugins.dvcs.JiraBitbucketOAuthPage;
import it.restart.com.atlassian.jira.plugins.dvcs.common.MagicVisitor;
import it.restart.com.atlassian.jira.plugins.dvcs.common.OAuth;
import it.restart.com.atlassian.jira.plugins.dvcs.common.PageController;

import com.atlassian.jira.pageobjects.JiraTestedProduct;

public class GithubOAuthPageController implements PageController<GithubOAuthPage>
{
    private final JiraTestedProduct jira;
    private GithubOAuthPage page;
    private OAuth oAuth;

    public GithubOAuthPageController(JiraTestedProduct jira)
    {
        this.jira = jira;
        this.page = new MagicVisitor(jira).visit(GithubOAuthPage.class);
    }

    @Override
    public GithubOAuthPage getPage()
    {
        return page;
    }

    public GithubOAuthPageController setupOAuth()
    {
        oAuth = page.addConsumer(jira.getProductInstance().getBaseUrl());
        JiraBitbucketOAuthPage jiraBitbucketOAuthPage = jira.visit(JiraBitbucketOAuthPage.class);
        jiraBitbucketOAuthPage.setCredentials(oAuth.key, oAuth.secret);
        return this;
    }

    public void removeOAuth()
    {
        page = new MagicVisitor(jira).visit(GithubOAuthPage.class, oAuth.applicationId);
        page.removeConsumer();
    }

}
