package it.restart.com.atlassian.jira.plugins.dvcs.test;

import static org.fest.assertions.api.Assertions.assertThat;
import it.restart.com.atlassian.jira.plugins.dvcs.OrganizationDiv;
import it.restart.com.atlassian.jira.plugins.dvcs.JiraLoginPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.RepositoriesPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.common.MagicVisitor;
import it.restart.com.atlassian.jira.plugins.dvcs.github.GithubLoginPage;
import it.restart.com.atlassian.jira.plugins.dvcs.github.GithubOAuthPageController;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.pageobjects.TestedProductFactory;

public class GithubOrganizationsTest implements BasicOrganizationTests, MissingCommitsTests
{
    private static JiraTestedProduct jira = TestedProductFactory.create(JiraTestedProduct.class);
    private static final String ACCOUNT_NAME = "jirabitbucketconnector";
    private GithubOAuthPageController ghOAuthController;
    
    @BeforeClass
    public void beforeClass()
    {
        // log in to JIRA 
        new JiraLoginPageController(jira).login();
        // log in to github
        new MagicVisitor(jira).visit(GithubLoginPage.class).doLogin();
        // setup up OAuth from github
        ghOAuthController = new GithubOAuthPageController(jira).setupOAuth();
    }
    
    @AfterClass
    public void afterClass()
    {
        // delete all organizations
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.getPage().deleteAllOrganizations();
        // remove OAuth in github
        ghOAuthController.removeOAuth();
        // log out from github
        new MagicVisitor(jira).visit(GithubLoginPage.class).doLogout();
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
        OrganizationDiv organization = rpc.addOrganization(RepositoriesPageController.GITHUB, ACCOUNT_NAME, false);
        
        assertThat(organization).isNotNull(); 
        assertThat(organization.getRepositories().size()).isEqualTo(4);  
    }

    @Override
    public void addOrganizationWaitForSync()
    {
        // TODO Auto-generated method stub
    }

    @Override
    public void addOrganizationInvalidUrl()
    {
        // TODO Auto-generated method stub
    }

}
