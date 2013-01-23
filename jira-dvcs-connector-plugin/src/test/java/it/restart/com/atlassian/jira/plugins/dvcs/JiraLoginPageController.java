package it.restart.com.atlassian.jira.plugins.dvcs;

import it.restart.com.atlassian.jira.plugins.dvcs.common.PageController;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.pageobjects.pages.JiraLoginPage;

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
        this.page = jira.getPageBinder().navigateToAndBind(JiraLoginPage.class);
    }
    
    public void login()
    {
        page.loginAsSystemAdminAndFollowRedirect(RepositoriesPage.class);
    }

}