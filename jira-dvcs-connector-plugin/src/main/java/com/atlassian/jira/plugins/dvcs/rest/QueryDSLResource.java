package com.atlassian.jira.plugins.dvcs.rest;

import com.atlassian.jira.plugins.dvcs.querydsl.v3.QBranchHeadMapping;
import com.atlassian.jira.plugins.dvcs.querydsl.v3.QChangesetMapping;
import com.atlassian.jira.plugins.dvcs.querydsl.v3.QRepositoryMapping;
import com.atlassian.jira.plugins.dvcs.querydsl.v3.QRepositoryToChangesetMapping;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.atlassian.pocketknife.api.querydsl.ConnectionProvider;
import com.atlassian.pocketknife.api.querydsl.QueryFactory;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLQuery;
import com.mysema.query.types.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Connection;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

@Path ("/QDSL")
@AnonymousAllowed
public class QueryDSLResource
{
    private final Logger log = LoggerFactory.getLogger(QueryDSLResource.class);

    private ConnectionProvider connectionProvider;
    private QueryFactory queryFactory;

    public QueryDSLResource()
    {

    }

    @Autowired
    public QueryDSLResource(ConnectionProvider connectionProvider, QueryFactory queryFactory)
    {
        this.connectionProvider = connectionProvider;
        this.queryFactory = queryFactory;
    }

    @GET
    @Path ("/repository/{repositoryId}/topo")
    public String getStuffForRepo(@PathParam ("repositoryId") String repositoryIdString) throws JSONException
    {
        Integer repositoryId = new Integer(repositoryIdString);
        Collection<String> heads = getHeads(repositoryId);
//        return "pocket!";
        final Connection connection = connectionProvider.borrowConnection();

        try
        {
            SQLQuery select = queryFactory.select(connection);
            QChangesetMapping mappingInstance = new QChangesetMapping("CSM", "", QChangesetMapping.AO_TABLE_NAME);
            final QRepositoryToChangesetMapping rtcMapping = new QRepositoryToChangesetMapping("RTC", "", QRepositoryToChangesetMapping.AO_TABLE_NAME);
            SQLQuery sql = select.from(mappingInstance)
                    .join(rtcMapping).on(mappingInstance.ID.eq(rtcMapping.CHANGESET_ID))
                    .where(rtcMapping.REPOSITORY_ID.eq(repositoryId));
            List<Tuple> result = sql.list(mappingInstance.ID, mappingInstance.NODE, mappingInstance.PARENTS_DATA);

            StringBuilder resultBuilder = new StringBuilder("result is: \n");

            for (Tuple tuple : result)
            {
                String resultLine = String.format("result is %s, %s, %s", new Object[] {
                        tuple.get(mappingInstance.ID), tuple.get(mappingInstance.NODE),
                        tuple.get(mappingInstance.PARENTS_DATA)
                });

                resultBuilder.append(resultLine);
                resultBuilder.append("\nParents:");

                String parents = tuple.get(mappingInstance.PARENTS_DATA);
                JSONArray parentsJson = new JSONArray(parents);
                for (int i = 0; i < parentsJson.length(); i++)
                {
                    resultBuilder.append(parentsJson.get(i));
                    resultBuilder.append("|");
                }
                resultBuilder.append("\n");
                log.info(resultLine);
            }
            return resultBuilder.toString();
        }
        finally
        {
            connectionProvider.returnConnection(connection);
        }
    }

    public Collection<String> getHeads(@PathParam ("repositoryId") Integer repositoryId)
    {
        final Connection connection = connectionProvider.borrowConnection();

        try
        {
            log.info("trying dsl query");
            SQLQuery select = queryFactory.select(connection);
            final QBranchHeadMapping mappingInstance = new QBranchHeadMapping("BH", "", QBranchHeadMapping.AO_TABLE_NAME);
            SQLQuery sql = select.from(mappingInstance)
                    .where(mappingInstance.REPOSITORY_ID.eq(repositoryId));
            log.info("sql {}", sql.toString());
            List<Tuple> result = sql.list(new Expression[] { mappingInstance.HEAD });

            return Collections2.transform(result, new Function<Tuple, String>()
            {
                @Override
                public String apply(@Nullable final Tuple input)
                {
                    return input.get(mappingInstance.HEAD);
                }
            });
        }
        finally
        {
            connectionProvider.returnConnection(connection);
        }
    }

    @GET
    @Path ("/repository/{repositoryId}/heads")
    public String getHeadsR(@PathParam ("repositoryId") String repositoryIdString)
    {
        Collection<String> heads = getHeads(new Integer(repositoryIdString));

        String result = "";
        for (String head : heads)
        {
            result += head;
            result += "\n";
        }
        return result;
    }

    @GET
    public String getStuff()
    {
//        return "pocket!";
        final Connection connection = connectionProvider.borrowConnection();

        try
        {
            log.info("trying dsl query");
            SQLQuery select = queryFactory.select(connection);
            QRepositoryMapping mappingInstance = new QRepositoryMapping("MR", "", QRepositoryMapping.AO_TABLE_NAME);
            SQLQuery sql = select.from(mappingInstance);
            log.info("sql {}", sql.toString());
            List<Tuple> result = sql.list(mappingInstance.ID, mappingInstance.NAME, mappingInstance.ORGANIZATION_ID, mappingInstance.SLUG);

            StringBuilder resultBuilder = new StringBuilder("result is: \n");

            for (Tuple tuple : result)
            {
                String resultLine = String.format("result is %s, %s, %s, %s", new Object[] {
                        tuple.get(mappingInstance.ID), tuple.get(mappingInstance.NAME),
                        tuple.get(mappingInstance.ORGANIZATION_ID), tuple.get(mappingInstance.SLUG)
                });
                resultBuilder.append(resultLine);
                resultBuilder.append("\n");
                log.info(resultLine);
            }
            return resultBuilder.toString();
        }
        finally
        {
            connectionProvider.returnConnection(connection);
        }
    }
}
