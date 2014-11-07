package com.atlassian.jira.plugins.dvcs.pageobjects.page;

import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;

public class TimeTrackingPage implements Page
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
    }

    public void deactivateTimeTrackingWithDefaults()
    {
        if (deactivateSubmit.isPresent())
        {
            deactivateSubmit.click();
        }
    }

    @Override
    public String getUrl()
    {
        return "/secure/admin/TimeTrackingAdmin!default.jspa";
    }
}
