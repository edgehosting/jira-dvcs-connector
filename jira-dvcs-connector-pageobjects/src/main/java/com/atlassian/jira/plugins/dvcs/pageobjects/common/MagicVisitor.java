package com.atlassian.jira.plugins.dvcs.pageobjects.common;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.pageobjects.Page;

/**
 * workaround for navigating and binding pages out of jira base url
 */
public class MagicVisitor
{
    private final JiraTestedProduct jira;

    public MagicVisitor(JiraTestedProduct jira)
    {
        this.jira = jira;
    }
    
    public  <P extends Page> P visit(Class<P> pageClass, Object... args)
    {
        return visit(jira.getPageBinder().delayedBind(pageClass, args).bind().getUrl(), pageClass, args);
    }
    
    public  <P extends Page> P visit(Class<P> pageClass)
    {
        return visit(jira.getPageBinder().delayedBind(pageClass).bind().getUrl(), pageClass);
    }

    public <P> P visit(String url, Class<P> pageClass)
    {
        jira.getTester().gotoUrl(url);
        return jira.getPageBinder().bind(pageClass);
    }

    public <P> P visit(String url, Class<P> pageClass, Object... args)
    {
        jira.getTester().gotoUrl(url);
        return jira.getPageBinder().bind(pageClass, args);
    }
}
