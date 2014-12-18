package com.atlassian.jira.plugins.dvcs.dao.impl.querydsl;

import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.pocketknife.api.querydsl.QueryFactory;
import com.atlassian.pocketknife.api.querydsl.SchemaProvider;
import com.google.common.collect.ImmutableList;
import com.mysema.query.Tuple;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator.BITBUCKET;
import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.when;

public class ChangesetQDSLByIssueKeyClosureTest
{
    private static final Integer CHANGESET_MAPPING_ID = 5;
    private static final Integer REPOSITORY_MAPPING_ID = 10;
    private static final String ISSUE_KEY = "HHH-123";

    private ChangesetQDSL changesetQDSL;

    @Mock
    private QueryFactory queryFactory;

    @Mock
    private SchemaProvider schemaProvider;

    @Mock
    private Tuple tuple;

    private ChangesetQDSL.ByIssueKeyClosure issueKeyProcesor;
    private Map<Integer, Changeset> changesetsById;
    private Changeset existingChangeset;

    @BeforeMethod
    public void setup()
    {
        MockitoAnnotations.initMocks(this);

        existingChangeset = new Changeset(REPOSITORY_MAPPING_ID, "sdkljf", "skldjfskldw", new Date());
        existingChangeset.setId(CHANGESET_MAPPING_ID);
        existingChangeset.getIssueKeys().add(ISSUE_KEY);

        changesetQDSL = new ChangesetQDSL(queryFactory, schemaProvider);
        changesetsById = new HashMap<Integer, Changeset>();

        when(schemaProvider.getSchema(argThat(any(String.class)))).thenReturn("something");
        issueKeyProcesor = new ChangesetQDSL.ByIssueKeyClosure(BITBUCKET, ImmutableList.of(ISSUE_KEY), schemaProvider, true);

        when(tuple.get(issueKeyProcesor.changesetMapping.ID)).thenReturn(CHANGESET_MAPPING_ID);
        when(tuple.get(issueKeyProcesor.changesetMapping.FILE_COUNT)).thenReturn(2);
        when(tuple.get(issueKeyProcesor.changesetMapping.VERSION)).thenReturn(3);
        when(tuple.get(issueKeyProcesor.changesetMapping.SMARTCOMMIT_AVAILABLE)).thenReturn(true);
        when(tuple.get(issueKeyProcesor.changesetMapping.DATE)).thenReturn(new Date());

        when(tuple.get(issueKeyProcesor.repositoryMapping.ID)).thenReturn(REPOSITORY_MAPPING_ID);

        when(tuple.get(issueKeyProcesor.issueToChangesetMapping.ISSUE_KEY)).thenReturn(ISSUE_KEY);
        // let the other values return null, they won't hurt

        changesetsById.put(CHANGESET_MAPPING_ID, existingChangeset);
    }

    @Test
    public void testSimplePopulateOnEmpty()
    {
        changesetsById.clear();
        issueKeyProcesor.getFoldFunction().apply(changesetsById, tuple);

        assertThat(changesetsById.size(), equalTo(1));
        assertThat(changesetsById.get(CHANGESET_MAPPING_ID).getRepositoryIds(), containsInAnyOrder(REPOSITORY_MAPPING_ID));
        assertThat(changesetsById.get(CHANGESET_MAPPING_ID).getIssueKeys(), containsInAnyOrder(ISSUE_KEY));
    }

    @Test
    public void testPopulateExistingChangesetOverlapOnRepoAndIssue()
    {
        issueKeyProcesor.getFoldFunction().apply(changesetsById, tuple);

        assertThat(changesetsById.size(), equalTo(1));
        assertThat(changesetsById.get(CHANGESET_MAPPING_ID).getRepositoryIds(), containsInAnyOrder(REPOSITORY_MAPPING_ID));
        assertThat(changesetsById.get(CHANGESET_MAPPING_ID).getIssueKeys(), containsInAnyOrder(ISSUE_KEY));
    }

    @Test
    public void testPopulateExistingChangesetNoOverlap()
    {
        final int existingRepoId = 100;
        existingChangeset.getRepositoryIds().clear();
        existingChangeset.getRepositoryIds().add(existingRepoId);

        final String existingIssueKey = "III-213";
        existingChangeset.getIssueKeys().clear();
        existingChangeset.getIssueKeys().add(existingIssueKey);
        issueKeyProcesor.getFoldFunction().apply(changesetsById, tuple);

        assertThat(changesetsById.size(), equalTo(1));
        assertThat(changesetsById.get(CHANGESET_MAPPING_ID).getRepositoryIds(), containsInAnyOrder(existingRepoId, REPOSITORY_MAPPING_ID));
        assertThat(changesetsById.get(CHANGESET_MAPPING_ID).getIssueKeys(), containsInAnyOrder(existingIssueKey, ISSUE_KEY));
    }
}
