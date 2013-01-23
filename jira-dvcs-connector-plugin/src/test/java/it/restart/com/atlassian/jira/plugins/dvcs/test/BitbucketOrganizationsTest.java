package it.restart.com.atlassian.jira.plugins.dvcs.test;

import static org.fest.assertions.api.Assertions.assertThat;
import it.restart.com.atlassian.jira.plugins.dvcs.JiraLoginPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.RepositoriesPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.bitbucket.BitbucketLoginPage;
import it.restart.com.atlassian.jira.plugins.dvcs.bitbucket.BitbucketOAuthPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.common.MagicBinder;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
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

    @BeforeMethod
    public void beforeMethod()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.getPage().deleteAllOrganizations();
    }
    
    @Override
    @Test
    public void addOrganization()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.addOrganization(RepositoriesPageController.ACCOUNT_TYPE_BITBUCKET, ACCOUNT_NAME, false);

        assertThat(rpc.getPage().getOrganization("bitbucket", ACCOUNT_NAME)).isNotNull(); 
        assertThat(rpc.getPage().getOrganization("bitbucket", ACCOUNT_NAME).getRepositories().size()).isEqualTo(4);  
    }
    
    @AfterClass
    public void afterClass()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.getPage().deleteAllOrganizations();
        bbOAuthController.removeOAuth();
        jira.getPageBinder().bind(BitbucketLoginPage.class).doLogout();
    }
    
}
