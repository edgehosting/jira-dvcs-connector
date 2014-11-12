package com.atlassian.jira.plugins.dvcs.dao.impl.queryDSL;

import com.atlassian.jira.plugins.dvcs.activeobjects.DvcsConnectorTableNameConverter;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.ChangesetMapping;
import com.atlassian.jira.plugins.dvcs.dao.impl.transform.ChangesetTransformer;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileDetails;
import com.atlassian.jira.plugins.dvcs.model.FileData;
import net.java.ao.test.converters.NameConverters;
import net.java.ao.test.jdbc.NonTransactional;
import org.junit.Test;

import static com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator.BITBUCKET;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

@NameConverters (table = DvcsConnectorTableNameConverter.class)
public class ChangesetQDSL_updateChangesetDBTest extends ChangesetQDSLDBTest
{
    public static final String FILES_JSON_WITH_DETAILS = "{\"count\":2,\"files\":["
            + "{\"filename\":\"file3\",\"status\":\"modified\",\"additions\":2,\"deletions\":0},"
            + "{\"filename\":\"file4\",\"status\":\"modified\",\"additions\":4,\"deletions\":1}]}";

    @Test
    @NonTransactional
    public void testNothingReturnedWithoutFilesData() throws Exception
    {
        changesetMappingWithIssue.setFilesData(null);
        changesetMappingWithIssue.save();
        int oldFileCount = changesetMappingWithIssue.getFileCount();

        changesetQDSL.updateChangesetMappingsThatHaveOldFileData(ISSUE_KEYS, BITBUCKET);

        assertThat(changesetMappingWithIssue.getFileCount(), equalTo(oldFileCount));
    }

    @Test
    @NonTransactional
    public void testNothingReturnedWithZeroFiles() throws Exception
    {
        changesetMappingWithIssue.setFileCount(0);
        changesetMappingWithIssue.save();
        int oldFileCount = changesetMappingWithIssue.getFileCount();

        changesetQDSL.updateChangesetMappingsThatHaveOldFileData(ISSUE_KEYS, BITBUCKET);

        assertThat(changesetMappingWithIssue.getFileCount(), equalTo(oldFileCount));
    }

    @Test
    @NonTransactional
    public void testUpdates1() throws Exception
    {
        changesetMappingWithIssue.setFilesData(FILES_JSON_WITH_DETAILS);
        changesetMappingWithIssue.setFileDetailsJson(null);
        changesetMappingWithIssue.setFileCount(0);
        changesetMappingWithIssue.save();

        int numberUpdated = changesetQDSL.updateChangesetMappingsThatHaveOldFileData(ISSUE_KEYS, BITBUCKET);
        assertThat(numberUpdated, equalTo(1));

        ChangesetMapping retrievedChangeSetMapping = entityManager.get(ChangesetMapping.class, changesetMappingWithIssue.getID());

        assertThat(retrievedChangeSetMapping.getFileCount(), equalTo(2));
        String expectedJson = ChangesetFileDetails.toJSON(ChangesetTransformer.transfromFileData(FileData.from(FILES_JSON_WITH_DETAILS, null)));
        String newJson = retrievedChangeSetMapping.getFileDetailsJson();
        assertThat(newJson, equalTo(expectedJson));
    }

    @Test
    @NonTransactional
    public void testUpdatesMany() throws Exception
    {
        ChangesetMapping secondMapping = changesetAOPopulator.createCSM("foo1", ISSUE_KEY, enabledRepository);
        updateChangesetWithFileDetailsThatNeedUpdating(secondMapping);

        ChangesetMapping thirdMapping = changesetAOPopulator.createCSM("foo2", ISSUE_KEY, enabledRepository);
        updateChangesetWithFileDetailsThatNeedUpdating(thirdMapping);

        ChangesetMapping fourthMapping = changesetAOPopulator.createCSM("foo3", ISSUE_KEY, enabledRepository);
        updateChangesetWithFileDetailsThatNeedUpdating(fourthMapping);

        // This one should not be picked up as the default values should be excluded from the query;
        ChangesetMapping ignoredCS = changesetAOPopulator.createCSM(changesetAOPopulator.getDefaultCSParams());
        ignoredCS.setNode("ignored");
        ignoredCS.setFileDetailsJson("foo");
        ignoredCS.setFilesData(null);
        ignoredCS.save();

        int numberUpdated = changesetQDSL.updateChangesetMappingsThatHaveOldFileData(ISSUE_KEYS, BITBUCKET);
        assertThat(numberUpdated, equalTo(3));
    }

    private void updateChangesetWithFileDetailsThatNeedUpdating(ChangesetMapping changesetMapping)
    {
        changesetMapping.setFilesData(FILES_JSON_WITH_DETAILS);
        changesetMapping.setFileDetailsJson(null);
        changesetMapping.setFileCount(0);
        changesetMapping.save();
    }
}
