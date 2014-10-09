package com.atlassian.jira.plugins.dvcs.pageobjects;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.pageobjects.pages.JiraLoginPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.RepositoriesPage;
import com.atlassian.pageobjects.Page;
import com.atlassian.jira.plugins.dvcs.pageobjects.common.PageController;

public class JiraLoginPageController implements PageController<JiraLoginPage>
{
    private final JiraLoginPage page;

    @Override
    public JiraLoginPage getPage()
    {
        return page;
    }

    public JiraLoginPageController(JiraTestedProduct jira)
    {
        this.page = jira.visit(JiraLoginPage.class);
    }
    
    public void login()
    {
        page.loginAsSystemAdminAndFollowRedirect(RepositoriesPage.class);
    }

    public <M extends Page>  void login(Class<M> followRedirect)
    {
        page.loginAsSystemAdminAndFollowRedirect(followRedirect);
    }
}
