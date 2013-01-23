package it.restart.com.atlassian.jira.plugins.dvcs;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.hamcrest.Matcher;
import org.openqa.selenium.By;

import com.atlassian.jira.plugins.dvcs.util.PageElementUtils;
import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.PageElementFinder;
import com.atlassian.pageobjects.elements.SelectElement;
import com.atlassian.pageobjects.elements.query.Poller;

/**
 * Represents the page to link repositories to projects
 */
public class RepositoriesPage implements Page
{
    @Inject
    private PageBinder pageBinder;

    @Inject
    private PageElementFinder elementFinder;


    @ElementBy(id = "aui-message-bar")
    private PageElement messageBarDiv;

    @ElementBy(id = "organization-list")
    private PageElement organizationsElement;
    
    @ElementBy(id = "repoEntry")
    private PageElement repoEntry;

    @ElementBy(id = "urlSelect")
    private SelectElement dvcsTypeSelect;

    @ElementBy(id = "organization")
    private PageElement organization;
    
    @ElementBy(id = "autoLinking")
    private PageElement autoLinkNewRepos;
    
    @ElementBy(id = "linkRepositoryButton")
    private PageElement linkRepositoryButton;
    
    @ElementBy(id = "Submit")
    private PageElement addOrgButton;
    
//    @ElementBy(className = "gh_messages")
//    private PageElement syncStatusDiv;

    @Override
    public String getUrl()
    {
        return "/secure/admin/ConfigureDvcsOrganizations!default.jspa";
    }
    
    public void addOrganisation(int accountType, String accountName, boolean autoSync)
    {
        linkRepositoryButton.click();
        waitFormBecomeVisible();
        dvcsTypeSelect.select(dvcsTypeSelect.getAllOptions().get(accountType));
        organization.clear().type(accountName);
        if (!autoSync)
        {
            autoLinkNewRepos.click();
        }
        addOrgButton.click();
    }

    public BitbucketOrganizationDiv getOrganization(String repositoryType, String repositoryName)
    {
        List<BitbucketOrganizationDiv> organizations = getOrganizations();
        for (BitbucketOrganizationDiv bitbucketOrganizationDiv : organizations)
        {
            if (repositoryType.equals(bitbucketOrganizationDiv.getRepositoryType())
                    && repositoryName.equals(bitbucketOrganizationDiv.getRepositoryName()))
            {
                return bitbucketOrganizationDiv;
            }
        }
        return null;
    }

    
    public List<BitbucketOrganizationDiv> getOrganizations()
    {
        List<BitbucketOrganizationDiv> list = new ArrayList<BitbucketOrganizationDiv>();
        for (PageElement orgContainer : organizationsElement.findAll(By.className("dvcs-orgdata-container")))
        {
            Poller.waitUntilTrue(orgContainer.find(By.className("dvcs-org-container")).timed().isVisible());
            list.add(pageBinder.bind(BitbucketOrganizationDiv.class, orgContainer));
        }
        return list;
    }

    public void deleteAllOrganizations()
    {
        List<BitbucketOrganizationDiv> orgs;
        while (!(orgs = getOrganizations()).isEmpty())
        {
            orgs.get(0).delete();
        }
    }

    public void assertThatErrorMessage(Matcher<String> matcher)
    {
        Poller.waitUntil(messageBarDiv.find(By.className("error")).timed().getText(), matcher);
    }

    protected void waitFormBecomeVisible()
    {
        Poller.waitUntilTrue("Expected add repository form to be visible", repoEntry.timed().isVisible());
    }

    /**
     * The current error status message
     * @return error status message
     */

    public String getErrorStatusMessage()
    {
        return messageBarDiv.find(By.className("error")).timed().getText().now();
    }

    public boolean containsRepositoryWithName(String askedRepositoryName)
    {
        // parsing following HTML:
        // <td class="dvcs-org-reponame">
        // <a href="...">browsermob-proxy</a>
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
                // parsing:
                // onclick="forceSync(90); AJS.$('.gh_messages.repository90').slideDown(); return false;"
                int openBraceIndex = onclickAttributeValue.indexOf('(');
                int closeBraceIndex = onclickAttributeValue.indexOf(')');

                return onclickAttributeValue.substring(openBraceIndex + 1, closeBraceIndex);
            }
        }
        return null;
    }

    public int getRepositoriesCount(int organisationIndex)
    {
        return getOrganizations().get(organisationIndex).getRepositories().size();
    }
}
