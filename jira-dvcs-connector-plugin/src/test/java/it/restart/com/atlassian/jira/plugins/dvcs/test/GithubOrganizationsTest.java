package it.restart.com.atlassian.jira.plugins.dvcs.test;

import static org.fest.assertions.api.Assertions.assertThat;
import it.restart.com.atlassian.jira.plugins.dvcs.JiraGithubOAuthPage;
import it.restart.com.atlassian.jira.plugins.dvcs.JiraLoginPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.OrganizationDiv;
import it.restart.com.atlassian.jira.plugins.dvcs.RepositoriesPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.common.MagicVisitor;
import it.restart.com.atlassian.jira.plugins.dvcs.common.OAuth;
import it.restart.com.atlassian.jira.plugins.dvcs.github.GithubLoginPage;
import it.restart.com.atlassian.jira.plugins.dvcs.github.GithubOAuthPage;

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
    private OAuth oAuth;
    
    @BeforeClass
    public void beforeClass()
    {
        // log in to JIRA 
        new JiraLoginPageController(jira).login();
        // log in to github
        new MagicVisitor(jira).visit(GithubLoginPage.class).doLogin();
        // setup up OAuth from github
        oAuth = new MagicVisitor(jira).visit(GithubOAuthPage.class).addConsumer(jira.getProductInstance().getBaseUrl());
        jira.visit(JiraGithubOAuthPage.class).setCredentials(oAuth.key, oAuth.secret);
    }

    @AfterClass
    public void afterClass()
    {
        // delete all organizations
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.getPage().deleteAllOrganizations();
        // remove OAuth in github
        new MagicVisitor(jira).visit(GithubOAuthPage.class, oAuth.applicationId).removeConsumer();
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
    @Test
    public void addOrganizationWaitForSync()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        OrganizationDiv organization = rpc.addOrganization(RepositoriesPageController.GITHUB, ACCOUNT_NAME, true);
        
        assertThat(organization).isNotNull(); 
        assertThat(organization.getRepositories().size()).isEqualTo(4);
        assertThat(organization.getRepositories().get(3).getMessage()).isEqualTo("Mon Feb 06 2012");

    }
    
    @Override
    @Test(expectedExceptions = AssertionError.class, expectedExceptionsMessageRegExp = ".*Error!\\nThe url \\[https://privatebitbucket.org\\] is incorrect or the server is not responding.*")
    public void addOrganizationInvalidUrl()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.addOrganization(RepositoriesPageController.GITHUB, "https://privatebitbucket.org/someaccount", false);
    }

    @Override
    @Test(expectedExceptions = AssertionError.class, expectedExceptionsMessageRegExp = "Invalid OAuth")
    public void addOrganizationInvalidOAuth()
    {
        try
        {
            jira.visit(JiraGithubOAuthPage.class).setCredentials("xxx","yyy");
            RepositoriesPageController rpc = new RepositoriesPageController(jira);
            OrganizationDiv organization = rpc.addOrganization(RepositoriesPageController.GITHUB, ACCOUNT_NAME, true);

            assertThat(organization).isNotNull(); 
            assertThat(organization.getRepositories().size()).isEqualTo(4);  
        } finally
        {
            jira.visit(JiraGithubOAuthPage.class).setCredentials(oAuth.key, oAuth.secret);
        }
    }

}
