package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import net.java.ao.Entity;
import net.java.ao.Preload;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.Table;

@Preload
@Table("IssueToChangeset")
public interface IssueToChangesetMapping extends Entity
{

    public static final String TABLE_NAME = "AO_E8B6CC_ISSUE_TO_CHANGESET";
    public static final String CHANGESET_ID = "CHANGESET_ID";
    public static final String ISSUE_KEY = "ISSUE_KEY";
    public static final String PROJECT_KEY = "PROJECT_KEY";


    ChangesetMapping getChangeset();
    @Indexed
    String getIssueKey();
    @Indexed
    String getProjectKey();

    void setChangeset(ChangesetMapping changeset);
    void setIssueKey(String issueKey);
    void setProjectKey(String projectKey);

}
