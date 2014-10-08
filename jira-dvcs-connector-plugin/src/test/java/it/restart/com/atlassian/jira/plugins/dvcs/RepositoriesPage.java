package it.restart.com.atlassian.jira.plugins.dvcs;

import com.atlassian.jira.plugins.dvcs.pageobjects.page.OAuthCredentials;
import com.atlassian.jira.plugins.dvcs.pageobjects.util.PageElementUtils;
import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.PageElementFinder;
import com.atlassian.pageobjects.elements.SelectElement;
import com.atlassian.pageobjects.elements.query.Poller;
import org.hamcrest.Matcher;
import org.openqa.selenium.By;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

import static com.atlassian.pageobjects.elements.timeout.TimeoutType.PAGE_LOAD;

/**
 * Represents the page to link repositories to projects
 */
public class RepositoriesPage implements Page
{
    /**
     * Logger for this class.
     */
    private Logger logger = LoggerFactory.getLogger(RepositoriesPage.class);
    
    @Inject
    private PageBinder pageBinder;

    @Inject
    private PageElementFinder elementFinder;

    /**
     * Injected {@link WebDriver} dependency.
     */
    @Inject
    private WebDriver webDriver;

    @ElementBy(id = "aui-message-bar")
    private PageElement messageBarDiv;

    @ElementBy(id = "organization-list", timeoutType = PAGE_LOAD)
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

    @ElementBy(className = "button-panel-submit-button")
    private PageElement addOrgButton;

    // ------------------ BB ------------------------
    @ElementBy(id = "oauthBbClientId")
    private PageElement oauthBbClientId;
    
    @ElementBy(id = "oauthBbSecret")
    private PageElement oauthBbSecret;
    
    // ------------------ GH ------------------------
    @ElementBy(id = "oauthClientId")
    private PageElement oauthClientId;
    
    @ElementBy(id = "oauthSecret")
    private PageElement oauthSecret;
    
    // ------------------ GHE ------------------------
    @ElementBy(id = "urlGhe")
    private PageElement urlGhe;
    
    @ElementBy(id = "oauthClientIdGhe")
    private PageElement oauthClientIdGhe;
    
    @ElementBy(id = "oauthSecretGhe")
    private PageElement oauthSecretGhe;
    
    @ElementBy(xpath="//button[contains(concat(' ', @class , ' '),' button-panel-submit-button ') and text()='Continue']")
    PageElement continueAddOrgButton;

    @Override
    public String getUrl()
    {
        return "/secure/admin/ConfigureDvcsOrganizations!default.jspa";
    }

    public void addOrganisation(int accountType, String accountName, String url, OAuthCredentials oAuthCredentials, boolean autoSync)
    {
        linkRepositoryButton.click();
        waitFormBecomeVisible();
        dvcsTypeSelect.select(dvcsTypeSelect.getAllOptions().get(accountType));
        organization.clear().type(accountName);
        
        switch (accountType)
        {
        case 0:
            oauthBbClientId.clear().type(oAuthCredentials.key);
            oauthBbSecret.clear().type(oAuthCredentials.secret);
            break;
        case 1:
            oauthClientId.clear().type(oAuthCredentials.key);
            oauthSecret.clear().type(oAuthCredentials.secret);
            break;
        case 2:
            urlGhe.clear().type(url);
            oauthClientIdGhe.clear().type(oAuthCredentials.key);
            oauthSecretGhe.clear().type(oAuthCredentials.secret);
            break;
        default:
            break;
        } 
        
        if (!autoSync)
        {
            autoLinkNewRepos.click();
        }
        addOrgButton.click();
    
        // dismiss any information alert
        try {
            webDriver.switchTo().alert().accept();
        } catch (NoAlertPresentException e) {
            // nothing to do
        }
    }

    public OrganizationDiv getOrganization(String organizationType, String organizationName)
    {
        List<OrganizationDiv> organizations = getOrganizations();
        for (OrganizationDiv organizationDiv : organizations)
        {
            if (organizationType.equals(organizationDiv.getOrganizationType())
                    && organizationName.equals(organizationDiv.getOrganizationName()))
            {
                return organizationDiv;
            }
        }
        return null;
    }

    public List<OrganizationDiv> getOrganizations()
    {
        List<OrganizationDiv> list = new ArrayList<OrganizationDiv>();
        for (PageElement orgContainer : organizationsElement.findAll(By.className("dvcs-orgdata-container")))
        {
            Poller.waitUntilTrue(orgContainer.find(By.className("dvcs-org-container")).timed().isVisible());
            list.add(pageBinder.bind(OrganizationDiv.class, orgContainer));
        }
        return list;
    }

    public void deleteAllOrganizations()
    {
        List<OrganizationDiv> orgs;
        while (!(orgs = getOrganizations()).isEmpty())
        {
            logger.info("Deleting organization: " + orgs.get(0).getOrganizationName() + ":" + orgs.get(0).getOrganizationType());
            orgs.get(0).delete();
        }
    }

    public void assertThatErrorMessage(Matcher<String> matcher)
    {
        Poller.waitUntil(messageBarDiv.find(By.className("aui-message-error")).timed().getText(), matcher);
    }

    protected void waitFormBecomeVisible()
    {
        Poller.waitUntilTrue("Expected add repository form to be visible", repoEntry.timed().isVisible());
    }

    /**
     * The current error status message
     * 
     * @return error status message
     */

    public String getErrorStatusMessage()
    {
        // accessing tag name as workaround for permission denied to access property 'nr@context' issue
        PageElementUtils.permissionDeniedWorkAround(messageBarDiv);

        return messageBarDiv.find(By.className("aui-message-error")).timed().getText().by(1000l);
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
