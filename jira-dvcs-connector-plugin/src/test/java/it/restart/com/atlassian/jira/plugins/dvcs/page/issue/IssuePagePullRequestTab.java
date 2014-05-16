package it.restart.com.atlassian.jira.plugins.dvcs.page.issue;

import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.elements.WebDriverElement;
import com.atlassian.pageobjects.elements.WebDriverElementMappings;
import com.atlassian.pageobjects.elements.WebDriverLocators;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.LinkedList;
import java.util.List;
import javax.inject.Inject;

/**
 * Represents Selenium abstraction of a "Pull Request" tab of an {@link IssuePage}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class IssuePagePullRequestTab extends WebDriverElement
{

    /**
     * Injected {@link PageBinder} dependency.
     */
    @Inject
    private PageBinder pageBinder;

    /**
     * Constructor.
     * 
     * @param locator
     * @param timeoutType
     */
    public IssuePagePullRequestTab(By locator, TimeoutType timeoutType)
    {
        super(locator, timeoutType);
    }

    /**
     * @return Returns activities displayed by this tab.
     */
    public List<IssuePagePullRequestTabActivity> getActivities()
    {
        List<IssuePagePullRequestTabActivity> result = new LinkedList<IssuePagePullRequestTabActivity>();

        By locator = By.xpath("div[not(contains(concat(' ', @class, ' '), 'message-container'))]");
        List<WebElement> webElements = waitForWebElement().findElements(locator);

        for (int i = 0; i < webElements.size(); i++)
        {
            WebElement webElement = webElements.get(i);

            if (webElement.getAttribute("class").matches(".*\\bactivity-item\\b.*")
                    && !webElement.getAttribute("class").matches(".*\\bcommit\\b.*"))
            {
                result.add(pageBinder.bind(WebDriverElementMappings.findMapping(IssuePagePullRequestTabActivityUpdate.class),
                        WebDriverLocators.list(webElements.get(i), locator, i, locatable), defaultTimeout));
            }
            else if (webElement.getAttribute("class").matches(".*\\bcomment\\b.*")) 
            {
                result.add(pageBinder.bind(WebDriverElementMappings.findMapping(IssuePagePullRequestTabActivityComment.class),
                        WebDriverLocators.list(webElements.get(i), locator, i, locatable), defaultTimeout));
            }
            else
            {
                result.add(pageBinder.bind(WebDriverElementMappings.findMapping(IssuePagePullRequestTabActivity.class),
                        WebDriverLocators.list(webElements.get(i), locator, i, locatable), defaultTimeout));
            }
        }

        return result;
    }

}
