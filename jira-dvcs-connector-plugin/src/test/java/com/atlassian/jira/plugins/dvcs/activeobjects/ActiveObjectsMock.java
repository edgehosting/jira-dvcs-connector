package com.atlassian.jira.plugins.dvcs.activeobjects;

import java.util.Arrays;

import net.java.ao.Query;

import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import com.atlassian.activeobjects.external.ActiveObjects;

/**
 * Mock's support for {@link ActiveObjects}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class ActiveObjectsMock
{

    /**
     * @param whereParameter
     * @return {@link ArgumentMatcher} which matches, if provided where parameter is contained in {@link Query#getWhereParams()}.
     */
    public static Query queryContainsWhereParameter(final Object whereParameter)
    {
        return Mockito.argThat(new ArgumentMatcher<Query>()
        {

            @Override
            public boolean matches(Object argument)
            {
                return Arrays.asList(((Query) argument).getWhereParams()).contains(whereParameter);
            }

        });
    }
}
