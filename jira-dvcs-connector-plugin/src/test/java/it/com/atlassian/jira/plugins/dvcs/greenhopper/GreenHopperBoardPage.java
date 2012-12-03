package it.com.atlassian.jira.plugins.dvcs.greenhopper;

import java.util.List;
import com.atlassian.jira.plugins.dvcs.util.PageElementUtils;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.webdriver.jira.page.JiraAbstractPage;

import org.openqa.selenium.By;

import static org.fest.assertions.api.Assertions.*;

/**
 * @author Martin Skurla
 */
public class GreenHopperBoardPage extends JiraAbstractPage
{
    @ElementBy(id="greenhopper_menu")
    PageElement greenHopperMenu;

    @ElementBy(id="rapidb_lnk_1_lnk")
    PageElement qaBoardMenuLink;

    @ElementBy(id="plan-toggle")
    PageElement boardPlanToggleViewButton;

    @ElementBy(id="ghx-backlog-container")
    PageElement backlogContainerDiv;

    @ElementBy(className="tabs-menu")
    PageElement openIssueTabsMenu;

    @ElementBy(id="ghx-tab-com-atlassian-jira-plugins-jira-bitbucket-connector-plugin-dvcs-commits-greenhopper-tab")
    PageElement issueCommitsIntegrationDiv;


    @Override
    public String getUrl() {
        return "/secure/Dashboard.jspa";
    }


    public void goToQABoard()
    {
        greenHopperMenu.click();
        qaBoardMenuLink.click();
        boardPlanToggleViewButton.click();
    }

    public void assertCommitsAppearOnIssue(String issueKey, int expectedNumberOfAssociatedCommits)
    {
        PageElement qa1Div  = PageElementUtils.findTagWithAttribute(backlogContainerDiv, "div", "data-issue-key", issueKey);
        PageElement qa1Link = PageElementUtils.findTagWithAttribute(qa1Div,              "a",   "title",          issueKey);

        qa1Link.click();

        PageElement commitsTabLink = PageElementUtils.findTagWithAttribute(openIssueTabsMenu, "li", "title", "commits")
                                                     .find(By.tagName("a"));

        commitsTabLink.click();

        List<PageElement> commits = issueCommitsIntegrationDiv.findAll(By.className("CommitContainer"));

        assertThat(commits).hasSize(expectedNumberOfAssociatedCommits);
    }
}
