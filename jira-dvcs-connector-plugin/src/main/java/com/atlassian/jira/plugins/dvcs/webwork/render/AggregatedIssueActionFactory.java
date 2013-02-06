package com.atlassian.jira.plugins.dvcs.webwork.render;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugin.issuetabpanel.IssueAction;

public class AggregatedIssueActionFactory implements IssueActionFactory
{
    private static final Logger log = LoggerFactory.getLogger(AggregatedIssueActionFactory.class);

    private final Map<Class<? extends Object>, IssueActionFactory> issueActionFactories = 
            new HashMap<Class<? extends Object>, IssueActionFactory>();    
    
    public AggregatedIssueActionFactory(IssueActionFactory... issueActionFactories)
    {
        for (IssueActionFactory issueActionFactory : issueActionFactories)
        {
            this.issueActionFactories.put(issueActionFactory.getSupportedClass(), issueActionFactory);
        }
    }

    @Override
    public IssueAction create(Object activityItem)
    {
        IssueActionFactory issueActionFactory = issueActionFactories.get(activityItem.getClass());
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

}