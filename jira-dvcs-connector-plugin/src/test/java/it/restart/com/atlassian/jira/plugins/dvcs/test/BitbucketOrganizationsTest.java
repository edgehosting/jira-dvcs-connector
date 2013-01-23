package it.restart.com.atlassian.jira.plugins.dvcs.test;

import static org.fest.assertions.api.Assertions.assertThat;
import it.restart.com.atlassian.jira.plugins.dvcs.JiraLoginPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.RepositoriesPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.bitbucket.BitbucketLoginPage;
import it.restart.com.atlassian.jira.plugins.dvcs.bitbucket.BitbucketOAuthPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.common.MagicBinder;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.pageobjects.TestedProductFactory;

public class BitbucketOrganizationsTest implements BasicOrganizationTests, MissingCommitsTests
{
    private static JiraTestedProduct jira = TestedProductFactory.create(JiraTestedProduct.class);
    private static final String ACCOUNT_NAME = "jirabitbucketconnector";
    private BitbucketOAuthPageController bbOAuthController;
    
    @BeforeClass
    public void beforeClass()
    {
        new JiraLoginPageController(jira).login();
        new MagicBinder(jira).navigateAndBind(BitbucketLoginPage.class).doLogin();
        bbOAuthController = new BitbucketOAuthPageController(jira).setupOAuth();
    }
    
    @Override
    @Test
    public void addOrganization()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.addOrganization(RepositoriesPageController.ACCOUNT_TYPE_BITBUCKET, ACCOUNT_NAME, false);
        assertThat(rpc.getPage().getRepositoriesCount(0)).isGreaterThan(2); 
    }
    
    @AfterClass
    public void afterClass()
    {
        bbOAuthController.removeOAuth();
        jira.getPageBinder().bind(BitbucketLoginPage.class).doLogout();
    }
    
}
