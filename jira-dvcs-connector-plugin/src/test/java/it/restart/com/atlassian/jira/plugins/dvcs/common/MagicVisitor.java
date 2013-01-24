package it.restart.com.atlassian.jira.plugins.dvcs.common;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.pageobjects.Page;

public class MagicVisitor
{

    private final JiraTestedProduct jira;

    public MagicVisitor(JiraTestedProduct jira)
    {
        this.jira = jira;
    }
    
    /**
     * workaround for navigating and binding pages out of jira
     */
    public  <P extends Page> P visit(Class<P> pageClass)
    {
        return visit(pageClass, jira.getPageBinder().delayedBind(pageClass).bind().getUrl());
    }

    public <P> P visit(Class<P> pageClass, String url)
    {
        jira.getTester().gotoUrl(url);
        return jira.getPageBinder().bind(pageClass);
    }
    
}
