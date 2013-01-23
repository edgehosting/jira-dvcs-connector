package it.restart.com.atlassian.jira.plugins.dvcs.common;

import com.atlassian.pageobjects.Page;

public interface PageController<T extends Page>
{
    T getPage();
}
