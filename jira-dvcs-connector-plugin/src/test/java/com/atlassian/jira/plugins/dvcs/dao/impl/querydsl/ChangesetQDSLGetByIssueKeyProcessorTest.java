package com.atlassian.jira.plugins.dvcs.dao.impl.querydsl;

import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.querydsl.v3.QChangesetMapping;
import com.atlassian.jira.plugins.dvcs.querydsl.v3.QIssueToChangesetMapping;
import com.atlassian.jira.plugins.dvcs.querydsl.v3.QOrganizationMapping;
import com.atlassian.jira.plugins.dvcs.querydsl.v3.QRepositoryMapping;
import com.atlassian.jira.plugins.dvcs.querydsl.v3.QRepositoryToChangesetMapping;
import com.atlassian.pocketknife.api.querydsl.ConnectionProvider;
import com.atlassian.pocketknife.api.querydsl.QueryFactory;
import com.atlassian.pocketknife.api.querydsl.SchemaProvider;
import com.mysema.query.Tuple;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.when;

public class ChangesetQDSLGetByIssueKeyProcessorTest
{
    private static final Integer CHANGESET_MAPPING_ID = 5;
    private static final Integer REPOSITORY_MAPPING_ID = 10;
    private static final String ISSUE_KEY = "HHH-123";

    private ChangesetQDSL changesetQDSL;

    @Mock
    private ConnectionProvider connectionProvider;

    @Mock
    private QueryFactory queryFactory;

    @Mock
    private SchemaProvider schemaProvider;

    @Mock
    private Tuple tuple;

    private ChangesetQDSL.GetByIssueKeyProcessor issueKeyProcesor;
    private ChangesetQDSL.ChangesetQueryMappings mappings;
    private Map<Integer, Changeset> changesetsById;
    private Changeset existingChangeset;

    @BeforeMethod
    public void setup()
    {
        MockitoAnnotations.initMocks(this);

        existingChangeset = new Changeset(REPOSITORY_MAPPING_ID, "sdkljf", "skldjfskldw", new Date());
        existingChangeset.setId(CHANGESET_MAPPING_ID);
        existingChangeset.getIssueKeys().add(ISSUE_KEY);

        changesetQDSL = new ChangesetQDSL(connectionProvider, queryFactory, schemaProvider);
        changesetsById = new HashMap<Integer, Changeset>();

        final QChangesetMapping changesetMapping = QChangesetMapping.withSchema(schemaProvider);
        final QIssueToChangesetMapping issueToChangesetMapping = QIssueToChangesetMapping.withSchema(schemaProvider);
        final QRepositoryToChangesetMapping rtcMapping = QRepositoryToChangesetMapping.withSchema(schemaProvider);
        final QRepositoryMapping repositoryMapping = QRepositoryMapping.withSchema(schemaProvider);
        final QOrganizationMapping orgMapping = QOrganizationMapping.withSchema(schemaProvider);

        mappings = changesetQDSL.new ChangesetQueryMappings(changesetMapping, issueToChangesetMapping,
                rtcMapping, repositoryMapping, orgMapping);

        issueKeyProcesor = changesetQDSL.new GetByIssueKeyProcessor(mappings, changesetsById);

        when(tuple.get(changesetMapping.ID)).thenReturn(CHANGESET_MAPPING_ID);
        when(tuple.get(changesetMapping.FILE_COUNT)).thenReturn(2);
        when(tuple.get(changesetMapping.VERSION)).thenReturn(3);
        when(tuple.get(changesetMapping.SMARTCOMMIT_AVAILABLE)).thenReturn(true);
        when(tuple.get(changesetMapping.DATE)).thenReturn(new Date());

        when(tuple.get(repositoryMapping.ID)).thenReturn(REPOSITORY_MAPPING_ID);

        when(tuple.get(issueToChangesetMapping.ISSUE_KEY)).thenReturn(ISSUE_KEY);
        // let the other values return null, they won't hurt

        changesetsById.put(CHANGESET_MAPPING_ID, existingChangeset);
    }

    @Test
    public void testSimplePopulateOnEmpty()
    {
        changesetsById.clear();
        issueKeyProcesor.apply(tuple);

        assertThat(changesetsById.size(), equalTo(1));
        assertThat(changesetsById.get(CHANGESET_MAPPING_ID).getRepositoryIds(), containsInAnyOrder(REPOSITORY_MAPPING_ID));
        assertThat(changesetsById.get(CHANGESET_MAPPING_ID).getIssueKeys(), containsInAnyOrder(ISSUE_KEY));
    }

    @Test
    public void testPopulateExistingChangesetOverlapOnRepoAndIssue()
    {
        issueKeyProcesor.apply(tuple);

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
        issueKeyProcesor.apply(tuple);

        assertThat(changesetsById.size(), equalTo(1));
        assertThat(changesetsById.get(CHANGESET_MAPPING_ID).getRepositoryIds(), containsInAnyOrder(existingRepoId, REPOSITORY_MAPPING_ID));
        assertThat(changesetsById.get(CHANGESET_MAPPING_ID).getIssueKeys(), containsInAnyOrder(existingIssueKey, ISSUE_KEY));
    }
}
