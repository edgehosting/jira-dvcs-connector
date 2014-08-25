package com.atlassian.jira.plugins.dvcs.matchers;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.java.ao.Query;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import java.util.List;
import javax.annotation.Nonnull;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;

/**
 * Hamcrest matchers for the {@link Query} object.
 */
public class QueryMatchers
{

    @Nonnull
    public static Matcher<Query> isSelect()
    {
        return withQuery(Query.QueryType.SELECT);
    }

    @Nonnull
    public static Matcher<Query> withQueryTypeThat(@Nonnull Matcher<Query.QueryType> queryTypeMatcher)
    {
        return new FeatureMatcher<Query, Query.QueryType>(queryTypeMatcher, "type that", "type") {

            @Override
            protected Query.QueryType featureValueOf(Query actual)
            {
                return actual.getType();
            }
        };
    }

    @Nonnull
    public static Matcher<Query> withQuery(@Nonnull Query.QueryType expectedType)
    {
        return withQueryTypeThat(Matchers.equalTo(expectedType));
    }

    @Nonnull
    public static Matcher<Query> withWhereThat(@Nonnull Matcher<String> whereMatcher)
    {
        return new FeatureMatcher<Query, String>(whereMatcher, "where that", "where") {

            @Override
            protected String featureValueOf(Query actual)
            {
                return actual.getWhereClause();
            }
        };
    }

    @Nonnull
    public static Matcher<Query> withWhereOnColumn(@Nonnull String column)
    {
        return withWhereThat(containsString(column));
    }

    @Nonnull
    public static Matcher<Query> withWhereOnColumns(@Nonnull String... columns)
    {
        List<Matcher<? super Query>> columnMatchers = Lists.newArrayListWithCapacity(columns.length);
        for (String column : columns)
        {
            columnMatchers.add(withWhereOnColumn(column));
        }
        return allOf(columnMatchers);
    }

    @Nonnull
    public static Matcher<Query> withWhereParamsThat(@Nonnull Matcher<Iterable<?>> whereParamsMatcher)
    {
        return new FeatureMatcher<Query, Iterable<?>>(whereParamsMatcher, "where params that", "whereParams") {

            @Override
            protected Iterable<?> featureValueOf(Query actual)
            {
                return ImmutableList.copyOf(actual.getWhereParams());
            }
        };
    }

}
