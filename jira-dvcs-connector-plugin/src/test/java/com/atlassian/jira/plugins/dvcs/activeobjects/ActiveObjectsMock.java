package com.atlassian.jira.plugins.dvcs.activeobjects;

import com.atlassian.activeobjects.external.ActiveObjects;
import net.java.ao.Query;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import java.util.Arrays;

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
