package it.restart.com.atlassian.jira.plugins.dvcs.github;

import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.elements.PageElementFinder;
import com.atlassian.jira.plugins.dvcs.pageobjects.common.OAuth;
import org.apache.commons.lang.StringUtils;
import org.openqa.selenium.By;

import javax.inject.Inject;

/**
 *
 */
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
        return hostUrl + "/settings/applications";
    }

    public void removeConsumer(OAuth oAuth)
    {
        String href = StringUtils.removeStart(oAuth.applicationId, hostUrl);

        pageElementFinder.find(By.xpath("//a[@href='" + href + "']")).click();

        pageBinder.bind(GithubOAuthPage.class).removeConsumer();
    }

}
