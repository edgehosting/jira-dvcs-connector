package it.restart.com.atlassian.jira.plugins.dvcs.github;

import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.elements.PageElementFinder;

import javax.inject.Inject;

/**
 *
 */
public class GithubOAuthApplication implements Page
{
    @Inject
    private PageElementFinder pageElementFinder;

    @Inject
    private PageBinder pageBinder;

    private final String appUri;

    public GithubOAuthApplication()
    {
        this("https://github.com");
    }

    public GithubOAuthApplication(String appUri)
    {
        this.appUri = appUri;
    }

    @Override
    public String getUrl()
    {
        return appUri;
    }

    public void removeConsumer()
    {
        pageBinder.bind(GithubOAuthPage.class).removeConsumer();

        //pageElementFinder.find(By.xpath("//a[@href='" + href + "']")).click();

        pageBinder.bind(GithubOAuthPage.class).removeConsumer();
    }
}
