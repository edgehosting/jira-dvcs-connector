package com.atlassian.jira.plugins.dvcs.pageobjects.page;

import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;

/**
 * @author Martin Skurla
 */
public class JiraAddProjectPage implements Page
{
    @ElementBy(id = "add-project-name")
    PageElement projectNameInput;

    @ElementBy(id = "add-project-key")
    PageElement projectKeyInput;

    @ElementBy(id = "add-project-submit")
    PageElement addProjectButton;


    @Override
    public String getUrl()
    {
        return "/secure/admin/AddProject!default.jspa";
    }


    public void createProject(String projectNameAndKey)
    {
        // Issue KEY must be 1st call otherwise javascript will copy the value from project name
        projectKeyInput .type(projectNameAndKey);
        projectNameInput.type(projectNameAndKey);

        addProjectButton.click();
    }
}
