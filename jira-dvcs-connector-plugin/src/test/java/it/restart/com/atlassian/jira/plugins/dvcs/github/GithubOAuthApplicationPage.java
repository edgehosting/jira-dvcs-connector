package it.restart.com.atlassian.jira.plugins.dvcs.github;

import com.atlassian.jira.plugins.dvcs.pageobjects.common.OAuth;
import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.PageElementFinder;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.webdriver.utils.WebDriverUtil;
import it.restart.com.atlassian.jira.plugins.dvcs.test.GithubTestHelper;
import org.apache.commons.lang.StringUtils;
import org.openqa.selenium.By;

import java.util.List;
import javax.inject.Inject;

import static it.restart.com.atlassian.jira.plugins.dvcs.test.GithubTestHelper.GITHUB_URL;

public class GithubOAuthApplicationPage implements Page
{
    @Inject
    private PageElementFinder pageElementFinder;

    @Inject
    private PageBinder pageBinder;

    private final String hostUrl;

    public GithubOAuthApplicationPage()
    {
        this(GITHUB_URL);
    }

    public GithubOAuthApplicationPage(String hostUrl)
    {
        this.hostUrl = hostUrl;
    }

    @Override
    public String getUrl()
    {
        // if github then we need to visit /settings/developers
        if (hostUrl.equalsIgnoreCase(GITHUB_URL))
        {
            return hostUrl + "/settings/developers";
        }
        else // github enterprise
        {
            return hostUrl + "/settings/applications";
        }

    }

    public void removeConsumer(final OAuth oAuth)
    {
        this.removeConsumer(StringUtils.removeStart(oAuth.applicationId, hostUrl));
    }

    public void removeConsumer(final String appUri)
    {
        removeConsumer(By.xpath("//a[@href='" + appUri + "']"));
    }

    public void removeConsumerForAppName(final String appName)
    {
        removeConsumer(By.linkText(appName));
    }

    public void removeConsumer(By bySelector)
    {
        removeConsumer(this, bySelector);
    }

    public List<PageElement> findOAthApplications(final String partialLinkText)
    {
        return pageElementFinder.findAll(By.partialLinkText(partialLinkText));
    }

    private static void removeConsumer(final GithubOAuthApplicationPage page, final By bySelector)
    {
        final PageElement link = page.pageElementFinder.find(bySelector);
        if (link.isPresent() && link.isVisible())
        {
            link.click();
            page.pageBinder.bind(GithubOAuthPage.class).removeConsumer();
        }
        else
        {
            // get next link
            final PageElement nextPageLink = page.pageElementFinder.find(By.className("next_page"));
            if (nextPageLink.isPresent() && nextPageLink.isVisible() && nextPageLink.getTagName().equalsIgnoreCase("a")
                    && nextPageLink.isEnabled())
            {
                nextPageLink.click();
                Poller.waitUntilTrue(page.pageElementFinder.find(By.className("table-list-bordered")).timed().isPresent());
                final GithubOAuthApplicationPage nextPage = page.pageBinder.bind(GithubOAuthApplicationPage.class);
                removeConsumer(nextPage, bySelector);
            }
            else
            {
                throw new RuntimeException("Can not find consumer application link");
            }
        }
    }
}
