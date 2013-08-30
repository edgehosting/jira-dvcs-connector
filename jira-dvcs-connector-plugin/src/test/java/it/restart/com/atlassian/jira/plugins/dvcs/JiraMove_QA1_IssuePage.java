package it.restart.com.atlassian.jira.plugins.dvcs;

import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;

public class JiraMove_QA1_IssuePage implements Page
{
    @ElementBy(id = "next_submit")
    private PageElement nextButton;

    @ElementBy(id = "project-field")
    private PageElement projectSelector;

    // for last step
    @ElementBy(id = "move_submit")
    private PageElement moveSubmit;

    // used binder for stepping
    private final PageBinder pageBinder;

    public JiraMove_QA1_IssuePage(PageBinder pageBinder)
    {
        super();
        this.pageBinder = pageBinder;
    }

    @Override
    public String getUrl()
    {
        return "/secure/MoveIssue!default.jspa?id=10000";
    }

    public JiraMove_QA1_IssuePage stepOne_typeProjectName(String fullProjectName)
    {
      //  this.pageBinder = pageBinder;
        projectSelector.type(fullProjectName);
        return bound();
    }

    public JiraMove_QA1_IssuePage clickNext()
    {
        nextButton.click();
        return bound();
    }

    private JiraMove_QA1_IssuePage bound()
    {
        return pageBinder.bind(this.getClass(), pageBinder);
    }

    public void submit()
    {
        moveSubmit.click();
    }
}