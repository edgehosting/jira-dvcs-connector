package com.atlassian.jira.plugins.dvcs.pageobjects.page;

import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;

public class JiraAddUserPage implements Page
{
    @ElementBy(id = "dvcs-bitucket-extension")
    PageElement dvcsExtensionsPanel;

    @Override
    public String getUrl()
    {
        return "/secure/admin/user/AddUser!default.jspa";
    }

    public void checkPanelPresented()
    {
        Poller.waitUntilTrue("Expected DVCS extension panel - Bitbucket grups", dvcsExtensionsPanel.timed().isVisible());
    }
}
