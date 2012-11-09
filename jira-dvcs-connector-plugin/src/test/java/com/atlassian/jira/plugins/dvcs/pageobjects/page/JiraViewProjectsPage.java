package com.atlassian.jira.plugins.dvcs.pageobjects.page;

import javax.inject.Inject;
import com.atlassian.jira.plugins.dvcs.util.PageElementUtils;
import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.webdriver.AtlassianWebDriver;

/**
 * @author Martin Skurla
 */
public class JiraViewProjectsPage implements Page
{
    @Inject
    AtlassianWebDriver atlassianWebDriver;

    @Inject
    PageBinder pageBinder;

    @ElementBy(id = "project-list")
    PageElement projectListTable;


    @Override
    public String getUrl()
    {
        return "/secure/project/ViewProjects.jspa";
    }


    public void deleteProject(String projectNameAndKey)
    {
        PageElement jiraProjectRow = PageElementUtils.findTagWithAttribute(projectListTable,
                                                                           "tr",
                                                                           "data-project-key",
                                                                           projectNameAndKey);

        if (jiraProjectRow.isPresent())
        {
            PageElement deleteProjectLink = PageElementUtils.findTagWithText(jiraProjectRow, "a", "Delete");
            deleteProjectLink.click();

            PageElementUtils.waitUntilPageUrlContains(atlassianWebDriver, "DeleteProject!default.jspa");

            DeleteProjectPage deleteProjectPage = pageBinder.bind(DeleteProjectPage.class);
            deleteProjectPage.deleteProject();
        }
    }


    public static class DeleteProjectPage implements Page
    {
        @ElementBy(id = "delete_submit")
        PageElement deleteProjectButton;

        @Override
        public String getUrl() {
            return "/secure/project/DeleteProject!default.jspa";
        }

        private void deleteProject()
        {
            deleteProjectButton.click();
        }
    }
}
