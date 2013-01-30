package it.restart.com.atlassian.jira.plugins.dvcs;

import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;

public class JiraAddUserPage implements Page
{
    @ElementBy(id = "dvcs-bitucket-extension")
    private PageElement dvcsExtensionsPanel;

    @Override
    public String getUrl()
    {
        return "/secure/admin/user/AddUser!default.jspa";
    }

    public PageElement getDvcsExtensionsPanel()
    {
        return dvcsExtensionsPanel;
    }
}
