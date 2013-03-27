package com.atlassian.jira.plugins.dvcs.activity;

import net.java.ao.ManyToMany;
import net.java.ao.schema.Table;

@Table("ACTIVITY_PR_UPDATE")
public interface RepositoryPullRequestUpdateActivityMapping extends RepositoryPullRequestActivityMapping
{
    String STATUS = "STATUS";

    String REMOTE_ID = "REMOTE_ID";

    // Status constants
    enum Status
    {
        APPROVED, OPENED, MERGED, DECLINED, REOPENED, UPDATED;
    }

    Status getStatus();

    @ManyToMany(RepositoryPullRequestUpdateActivityToCommitMapping.class)
    RepositoryCommitMapping[] getCommits();

    void setStatus(Status status);

    /**
     * @return Remote ID association.
     */
    String getRemoteId();

    /**
     * @param remoteId
     *            {@link #getRemoteId()}
     */
    void setRemoteId(String remoteId);

}
