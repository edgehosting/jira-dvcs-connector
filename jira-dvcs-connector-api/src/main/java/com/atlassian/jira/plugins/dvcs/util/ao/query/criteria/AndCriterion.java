package com.atlassian.jira.plugins.dvcs.util.ao.query.criteria;

import com.atlassian.jira.plugins.dvcs.util.ao.query.DefaultQueryNode;
import com.atlassian.jira.plugins.dvcs.util.ao.query.QueryContext;
import com.atlassian.jira.plugins.dvcs.util.ao.query.QueryNode;

/**
 * Criteria implementation over AND condition
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class AndCriterion extends DefaultQueryNode implements QueryCriterion
{

    /**
     * @see #AndCriteria(QueryNode[])
     */
    private final QueryCriterion[] participants;

    /**
     * Constructor.
     * 
     * @param criterias
     *            AND participants
     */
    public AndCriterion(QueryCriterion[] criterias)
    {
        this.participants = criterias;
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
                    where.append(" AND ");
                }

                participant.buildWhere(context, where);
            }

        }

    }

}
