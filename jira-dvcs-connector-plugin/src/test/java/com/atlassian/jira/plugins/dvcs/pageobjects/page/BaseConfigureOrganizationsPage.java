package com.atlassian.jira.plugins.dvcs.pageobjects.page;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.hamcrest.Matcher;
import org.openqa.selenium.By;

import com.atlassian.jira.plugins.dvcs.pageobjects.component.BitBucketOrganization;
import com.atlassian.jira.plugins.dvcs.util.PageElementUtils;
import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.PageElementFinder;
import com.atlassian.pageobjects.elements.SelectElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.webdriver.jira.JiraTestedProduct;

/**
 * Represents the page to link repositories to projects
 */
public abstract class BaseConfigureOrganizationsPage implements Page
{
    @Inject
    PageBinder pageBinder;

    @Inject
    PageElementFinder elementFinder;

    @ElementBy(id = "linkRepositoryButton")
    PageElement linkRepositoryButton;

    @ElementBy(id = "Submit")
    PageElement addOrgButton;

    @ElementBy(className = "gh_messages")
    PageElement syncStatusDiv;

    @ElementBy(id = "aui-message-bar")
    PageElement messageBarDiv;

    @ElementBy(id = "organization")
    PageElement organization;

    @ElementBy(id = "organization-list")
    PageElement organizationsElement;

    @ElementBy(id = "autoLinking")
    PageElement autoLinkNewRepos;

    @ElementBy(id = "urlSelect")
    SelectElement dvcsTypeSelect;

    protected JiraTestedProduct jiraTestedProduct;


    @Override
    public String getUrl()
    {
        return "/secure/admin/ConfigureDvcsOrganizations!default.jspa";
    }


    public List<BitBucketOrganization> getOrganizations()
    {
        List<BitBucketOrganization> list = new ArrayList<BitBucketOrganization>();

        for (PageElement orgContainer : organizationsElement.findAll(By.className("dvcs-orgdata-container"))) {

        	// orgContainer.find(By.className("dvcs-org-container")).click();
        	 Poller.waitUntilTrue(orgContainer.find(By.className("dvcs-org-container")).timed().isVisible());

             list.add(pageBinder.bind(BitBucketOrganization.class, orgContainer));

    	}

        return list;
    }


    public BaseConfigureOrganizationsPage deleteAllOrganizations()
    {
        List<BitBucketOrganization> orgs;
        while (!(orgs = getOrganizations()).isEmpty())
        {
            orgs.get(0).delete();
        }

    	return this;
    }

    /**
     * Whether a repository is currently linked to a given project
     *
     * @param projectKey The JIRA project key
     * @param url        The repository url
     * @return True if repository is linked, false otherwise
     */
   /* public boolean isRepositoryPresent(String projectKey, String url)
    {
        boolean commitFound = false;
        for (BitBucketRepository repo : getRepositories())
        {
            if (repo.getProjectKey().equals(projectKey) && repo.getUrl().equals(url))
            {
                commitFound = true;
                break;
            }
        }

        return commitFound;
    }*/

    /**
     * @param matcher
     */
    public void assertThatSyncMessage(Matcher<String> matcher)
    {
        Poller.waitUntil(syncStatusDiv.timed().getText(), matcher);
    }

    public void assertThatSuccessMessage(Matcher<String> matcher)
    {
        Poller.waitUntil(messageBarDiv.find(By.className("success")).timed().getText(), matcher);
    }

    public void assertThatWarningMessage(Matcher<String> matcher)
    {
        Poller.waitUntil(messageBarDiv.find(By.className("warning")).timed().getText(), matcher);
    }

    public void assertThatErrorMessage(Matcher<String> matcher)
    {
        Poller.waitUntil(messageBarDiv.find(By.className("error")).timed().getText(), matcher);
    }

    protected void checkSyncProcessSuccess()
    {
        // maybe we should do the rest call to server
        // to find out the status of syncing
        boolean syncFinished;
        do {
            // TODO the idea for waiting for all icons not to have "running" class doesn't work because
            // the javascript doesn't run onLoad stuff. 
            // The onload is broken by Raphael trying to do something with svg icons.
            // The error in the js console:
            //    this.join is not a function
            //    [Break on this error] return this.join(",").replace(p2s, "$1"); 
            sleep(1000);
            syncFinished = true;
            List<PageElement> syncIcons = organizationsElement.findAll(By.className("syncrepoicon"));
            System.out.println("Found some syncicons: " +syncIcons.size());
            
            for (PageElement syncIcon : syncIcons)
            {
                if (syncIcon.hasClass("running"))
                {
                    syncFinished = false;
                    System.out.println("Running");
                } else
                {
                    System.out.println("Not Running");
                }
            }
            
        } while (!syncFinished);
        // syncing is now finished. TODO check for errors
    }


    private void sleep(long milis)
    {
        try
        {
            Thread.sleep(milis);
        } catch (InterruptedException e)
        {
            // ignore
        }
    }

    protected void waitFormBecomeVisible()
    {
        jiraTestedProduct.getTester().getDriver().waitUntilElementIsVisible(By.id("repoEntry"));
    }

    /**
     * The current error status message
     *
     * @return error status message
     */

    public String getErrorStatusMessage()
    {
        return messageBarDiv.find(By.className("error")).timed().getText().now();
    }

    public abstract BaseConfigureOrganizationsPage addOrganizationFailingStep1(String url);

    public abstract BaseConfigureOrganizationsPage addRepoToProjectFailingStep2();

    public abstract BaseConfigureOrganizationsPage addRepoToProjectFailingPostcommitService(String url);

    public abstract BaseConfigureOrganizationsPage addOrganizationSuccessfully(String organizationAccount, boolean autosync);


    public void setJiraTestedProduct(JiraTestedProduct jiraTestedProduct)
    {
        this.jiraTestedProduct = jiraTestedProduct;
    }

    public void clearForm()
    {

    }


    public boolean containsRepositoryWithName(String askedRepositoryName)
    {
        // parsing following HTML:
        // <td class="dvcs-org-reponame">
        //     <a href="...">browsermob-proxy</a>
        // </td>

        for (PageElement repositoryNameTableRow : elementFinder.findAll(By.className("dvcs-org-reponame")))
        {
            String repositoryName = repositoryNameTableRow.find(By.tagName("a")).getText();

            if (repositoryName.equals(askedRepositoryName))
            {
                return true;
            }
        }

        return false;
    }


    public String getRepositoryIdFromRepositoryName(String queriedRepositoryName)
    {
        for (PageElement repositoryRow : elementFinder.findAll(By.className("dvcs-repo-row")))
        {
            String repositoryName = repositoryRow.find(By.className("dvcs-org-reponame")).find(By.tagName("a")).getText();

            if (repositoryName.equals(queriedRepositoryName))
            {
                PageElement syncRepoLink = PageElementUtils.findTagWithAttribute(repositoryRow, "a", "onclick");
                
                String onclickAttributeValue = syncRepoLink.getAttribute("onclick");
                // parsing: onclick="forceSync(90); AJS.$('.gh_messages.repository90').slideDown(); return false;"
                int openBraceIndex  = onclickAttributeValue.indexOf('(');
                int closeBraceIndex = onclickAttributeValue.indexOf(')');

                return onclickAttributeValue.substring(openBraceIndex + 1, closeBraceIndex);
            }
        }

        return null;
    }
}
