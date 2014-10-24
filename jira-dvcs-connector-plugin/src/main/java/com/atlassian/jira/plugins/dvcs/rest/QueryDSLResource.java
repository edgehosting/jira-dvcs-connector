package com.atlassian.jira.plugins.dvcs.rest;

import com.atlassian.jira.plugins.dvcs.querydsl.v3.QRepositoryMapping;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.atlassian.pocketknife.api.querydsl.ConnectionProvider;
import com.atlassian.pocketknife.api.querydsl.QueryFactory;
import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Connection;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

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
