package com.atlassian.jira.plugins.dvcs.pageobjects.common;

import com.atlassian.pageobjects.Page;

public interface PageController<T extends Page>
{
    T getPage();
}
