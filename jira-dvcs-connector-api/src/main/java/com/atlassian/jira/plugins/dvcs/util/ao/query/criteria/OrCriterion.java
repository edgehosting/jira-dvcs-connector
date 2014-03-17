package com.atlassian.jira.plugins.dvcs.util.ao.query.criteria;

import com.atlassian.jira.plugins.dvcs.util.ao.query.DefaultQueryNode;
import com.atlassian.jira.plugins.dvcs.util.ao.query.QueryContext;
import com.atlassian.jira.plugins.dvcs.util.ao.query.QueryNode;

/**
 * {@link QueryNode} implementation over OR condition.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class OrCriterion extends DefaultQueryNode implements QueryCriterion
{

    /**
     * @see #OrCriteria(QueryNode[])
     */
    private final QueryCriterion[] participants;

    /**
     * Constructor.
     * 
     * @param participants
     *            of OR criteria
     */
    public OrCriterion(QueryCriterion[] participants)
    {
        this.participants = participants;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void buildWhere(QueryContext context, StringBuilder where)
    {
        if (participants == null)
        {
            // nothing to do

        } else if (participants.length == 1)
        {
            participants[0].buildWhere(context, where);

        } else
        {
            int previousLength = where.length();
            for (QueryNode participant : participants)
            {
                if (previousLength != where.length())
                {
                    previousLength = where.length();
                    where.append(" OR ");
                }

                participant.buildWhere(context, where);
            }

        }
    }
}
