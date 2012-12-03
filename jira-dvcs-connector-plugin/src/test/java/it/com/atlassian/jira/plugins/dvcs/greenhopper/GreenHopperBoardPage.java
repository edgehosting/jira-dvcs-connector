package it.com.atlassian.jira.plugins.dvcs.greenhopper;

import java.util.List;
import com.atlassian.jira.plugins.dvcs.util.PageElementUtils;
import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;

import org.openqa.selenium.By;

import static org.fest.assertions.api.Assertions.*;

/**
 * @author Martin Skurla
 */
public class GreenHopperBoardPage implements Page
{
    @ElementBy(id="plan-toggle")
    PageElement boardPlanToggleViewButton;

    @ElementBy(tagName="body")
    PageElement bodyElement;


    @Override
    public String getUrl()
    {
        return "/secure/RapidBoard.jspa?rapidView=1&useStoredSettings=true";
    }


    public void goToQABoardPlan()
    {
        boardPlanToggleViewButton.click();
    }

    public void assertCommitsAppearOnIssue(String issueKey, int expectedNumberOfAssociatedCommits)
    {
        PageElement backlogContainerDiv = bodyElement.find(By.className("ghx-backlog-container"));

        PageElement qa1Div  = PageElementUtils.findTagWithAttribute(backlogContainerDiv, "div", "data-issue-key", issueKey);
        PageElement qa1Link = PageElementUtils.findTagWithAttribute(qa1Div,              "a",   "title",          issueKey);

        qa1Link.click();

        PageElement openIssueTabsMenu = bodyElement.find(By.className("tabs-menu"));
        Poller.waitUntilTrue(openIssueTabsMenu.timed().isVisible());
        PageElement commitsTabLink = PageElementUtils.findTagWithAttribute(openIssueTabsMenu, "li", "title", "Commits")
                                                     .find(By.tagName("a"));

        commitsTabLink.click();

        PageElement issueCommitsIntegrationDiv = bodyElement.find(By.id("ghx-tab-com-atlassian-jira-plugins-jira-bitbucket-connector-plugin-dvcs-commits-greenhopper-tab"));
        List<PageElement> commits = issueCommitsIntegrationDiv.findAll(By.className("CommitContainer"));

        assertThat(commits).hasSize(expectedNumberOfAssociatedCommits);
    }
}
