package com.atlassian.jira.plugins.dvcs.pageobjects.page;

import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;

public class TimeTrackingAdminPage implements Page
{
    @ElementBy (id = "activate_submit")
    private PageElement activateSubmit;

    @ElementBy (id = "deactivate_submit")
    private PageElement deactivateSubmit;

    public void activateTimeTrackingWithDefaults()
    {
        if (activateSubmit.isPresent())
        {
            activateSubmit.click();
        }
        else
        {
            throw new AssertionError("To activate time tracking the activate_submit button should be present. "
                    + "Check that you have not previously activated time tracking");
        }
    }

    public void deactivateTimeTrackingWithDefaults()
    {
        if (deactivateSubmit.isPresent())
        {
            deactivateSubmit.click();
        }
        else
        {
            throw new AssertionError("To deactivate time tracking the deactivate_submit button should be present. "
                    + "Check that you have not previously deactivated time tracking");
        }
    }

    @Override
    public String getUrl()
    {
        return "/secure/admin/TimeTrackingAdmin!default.jspa";
    }
}
