package com.atlassian.jira.plugins.dvcs.dao.impl.queryDSL;

import com.atlassian.jira.plugins.dvcs.dao.impl.transform.ChangesetTransformer;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileDetails;
import com.atlassian.jira.plugins.dvcs.model.FileData;
import com.atlassian.jira.plugins.dvcs.util.MockitoTestNgListener;
import com.atlassian.pocketknife.api.querydsl.QueryFactory;
import com.mysema.query.sql.RelationalPath;
import com.mysema.query.sql.dml.SQLUpdateClause;
import com.mysema.query.types.Path;
import com.mysema.query.types.Predicate;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.sql.Connection;

import static com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator.BITBUCKET;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Listeners (MockitoTestNgListener.class)
public class ChangesetQDSL_buildUpdateChangesetFileDetails
{
    public static final String FILES_JSON_WITH_DETAILS = "{\"count\":2,\"files\":["
            + "{\"filename\":\"file3\",\"status\":\"modified\",\"additions\":2,\"deletions\":0},"
            + "{\"filename\":\"file4\",\"status\":\"modified\",\"additions\":4,\"deletions\":1}]}";

    public static final String FILES_JSON_WITH_DETAILS_AT_MAX = "{\"count\":6,\"files\":["
            + "{\"filename\":\"file3\",\"status\":\"modified\",\"additions\":2,\"deletions\":0},"
            + "{\"filename\":\"file5\",\"status\":\"modified\",\"additions\":2,\"deletions\":0},"
            + "{\"filename\":\"file6\",\"status\":\"modified\",\"additions\":2,\"deletions\":0},"
            + "{\"filename\":\"file7\",\"status\":\"modified\",\"additions\":2,\"deletions\":0},"
            + "{\"filename\":\"file8\",\"status\":\"modified\",\"additions\":2,\"deletions\":0},"
            + "{\"filename\":\"file4\",\"status\":\"modified\",\"additions\":4,\"deletions\":1}]}";

    @InjectMocks
    private ChangesetQDSL changesetQDSL;

    @Mock
    private QueryFactory queryFactory;

    @Mock
    private SQLUpdateClause sqlUpdateClause;

    private ArgumentCaptor<Object> sqlSetCaptor;

    @Mock
    private Connection connection;

    private FileData fileData;

    private String expectedJsonFromFileData;

    @BeforeMethod
    public void setup()
    {
        sqlSetCaptor = ArgumentCaptor.forClass(Object.class);
        when(sqlUpdateClause.set(any(Path.class), sqlSetCaptor.capture())).thenReturn(sqlUpdateClause);
        when(sqlUpdateClause.setNull(any(Path.class))).thenReturn(sqlUpdateClause);
        when(sqlUpdateClause.where(any(Predicate.class))).thenReturn(sqlUpdateClause);
        when(queryFactory.update(any(Connection.class), any(RelationalPath.class))).thenReturn(sqlUpdateClause);
        fileData = fileData.from(FILES_JSON_WITH_DETAILS, null);
        expectedJsonFromFileData = ChangesetFileDetails.toJSON(ChangesetTransformer.transfromFileData(fileData));
    }

    @Test
    public void testBitbucketButNonMaxWithJson()
    {
        changesetQDSL.buildUpdateChangesetFileDetails(connection, BITBUCKET, null, fileData, "a", 1);
        assertThat(2, equalTo(sqlSetCaptor.getAllValues().size()));
        assertThat((Integer) sqlSetCaptor.getAllValues().get(0), equalTo(2));
        assertThat((String) sqlSetCaptor.getAllValues().get(1), equalTo(expectedJsonFromFileData));
    }

    @Test
    public void testBitbucketAndMaxFiles()
    {
        fileData = fileData.from(FILES_JSON_WITH_DETAILS_AT_MAX, null);
        changesetQDSL.buildUpdateChangesetFileDetails(connection, BITBUCKET, null, fileData, "a", 1);
        verify(sqlUpdateClause).setNull(any(Path.class));
    }

    @Test
    public void testBitbucketAndFileDataAndNewJson()
    {
        changesetQDSL.buildUpdateChangesetFileDetails(connection, BITBUCKET, "foo", fileData, "a", 1);
        verify(sqlUpdateClause).set(any(Path.class), any(Object.class));
    }

    @Test
    public void testNonBitbucketWithFileDataAndNewJson()
    {
        changesetQDSL.buildUpdateChangesetFileDetails(connection, "foo", "foo", fileData, "a", 1);
        verify(sqlUpdateClause).set(any(Path.class), any(Object.class));
    }

    @Test
    public void testNonBitbucketWithFileDataNoJson()
    {
        changesetQDSL.buildUpdateChangesetFileDetails(connection, "foo", null, fileData, "a", 1);
        assertThat(2, equalTo(sqlSetCaptor.getAllValues().size()));
        assertThat((Integer) sqlSetCaptor.getAllValues().get(0), equalTo(2));
        assertThat((String) sqlSetCaptor.getAllValues().get(1), equalTo(expectedJsonFromFileData));
    }
}
