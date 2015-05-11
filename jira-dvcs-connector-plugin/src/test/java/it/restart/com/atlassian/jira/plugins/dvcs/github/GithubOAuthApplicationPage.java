package it.restart.com.atlassian.jira.plugins.dvcs.github;

import com.atlassian.jira.plugins.dvcs.pageobjects.common.OAuth;
import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.PageElementFinder;
import org.apache.commons.lang.StringUtils;
import org.openqa.selenium.By;

import java.util.List;
import javax.inject.Inject;

public class GithubOAuthApplicationPage implements Page
{
    @Inject
    private PageElementFinder pageElementFinder;

    @Inject
    private PageBinder pageBinder;

    private final String hostUrl;

    public GithubOAuthApplicationPage()
    {
        this("https://github.com");
    }

    public GithubOAuthApplicationPage(String hostUrl)
    {
        this.hostUrl = hostUrl;
    }

    @Override
    public String getUrl()
    {
        // appUri is not found in /settings/applications instead it is found in /settings/developers
        return hostUrl + "/settings/developers";
    }

    public void removeConsumer(final OAuth oAuth)
    {
        this.removeConsumer(StringUtils.removeStart(oAuth.applicationId, hostUrl));
    }

    public void removeConsumer(final String appUri)
    {
        removeConsumer(this, By.xpath("//a[@href='" + appUri + "']"));
    }

    public void removeConsumerForAppName(final String appName)
    {
        removeConsumer(this, By.linkText(appName));
    }

    public List<PageElement> findOAthApplications(final String partialLinkText)
    {
        return pageElementFinder.findAll(By.partialLinkText(partialLinkText));
    }

    private static void removeConsumer(final GithubOAuthApplicationPage page, final By by)
    {
        final PageElement link = page.pageElementFinder.find(by);
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
                final GithubOAuthApplicationPage nextPage = page.pageBinder.bind(GithubOAuthApplicationPage.class);
                removeConsumer(nextPage, by);
            }
            else
            {
                throw new RuntimeException("Can not find consumer application link");
            }
        }
    }
}
