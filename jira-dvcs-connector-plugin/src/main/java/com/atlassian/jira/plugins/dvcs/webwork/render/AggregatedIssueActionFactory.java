package com.atlassian.jira.plugins.dvcs.webwork.render;

import com.atlassian.jira.plugin.issuetabpanel.IssueAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class AggregatedIssueActionFactory<E> implements IssueActionFactory
{
    private static final Logger log = LoggerFactory.getLogger(AggregatedIssueActionFactory.class);

    private final Set<IssueActionFactory> issueActionFactories = new HashSet<IssueActionFactory>();

    @Autowired
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

}
