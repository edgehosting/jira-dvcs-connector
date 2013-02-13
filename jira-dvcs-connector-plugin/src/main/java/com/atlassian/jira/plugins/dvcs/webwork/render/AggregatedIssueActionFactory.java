package com.atlassian.jira.plugins.dvcs.webwork.render;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugin.issuetabpanel.IssueAction;

public class AggregatedIssueActionFactory<E> implements IssueActionFactory
{
    private static final Logger log = LoggerFactory.getLogger(AggregatedIssueActionFactory.class);

    private final Set<IssueActionFactory> issueActionFactories = new HashSet<IssueActionFactory>(); 
    
    public AggregatedIssueActionFactory(IssueActionFactory... issueActionFactories)
    {
        for (IssueActionFactory issueActionFactory : issueActionFactories)
        {
            this.issueActionFactories.add(issueActionFactory);
        }
    }

    private IssueActionFactory getIssueActionFactory(Object activityItem)
    {
        for (IssueActionFactory issueActionFactory : issueActionFactories)
        {
            if (issueActionFactory.getSupportedClass().isAssignableFrom(activityItem.getClass()))
            {
                return issueActionFactory;
            }
        }
        return null;
    }

    @Override
    public IssueAction create(Object activityItem)
    {
        IssueActionFactory issueActionFactory = getIssueActionFactory(activityItem);

        if (issueActionFactory == null)
        {
            log.error("No IssueActionFactory found for class "+ activityItem.getClass());
            return null;
        }
        return issueActionFactory.create(activityItem);
    }
 

    @Override
    public Class<? extends Object> getSupportedClass()
    {
        throw new UnsupportedOperationException();
    }
    
    public static void main(String[] args)
    {
        System.out.println(IssueActionFactory.class.isAssignableFrom(AggregatedIssueActionFactory.class));
        System.out.println(AggregatedIssueActionFactory.class.isAssignableFrom(IssueActionFactory.class));
    }

}