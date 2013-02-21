package com.atlassian.jira.plugins.dvcs.activity;

import net.java.ao.OneToMany;
import net.java.ao.schema.Table;

@Table("PR_UPDATE")
public interface RepositoryActivityPullRequestUpdateMapping extends RepositoryActivityPullRequestMapping
{
    String STATUS = "STATUS";

    String REMOTE_ID = "REMOTE_ID";

    // Status constants
    enum Status
    {
        APPROVED, OPENED, MERGED, DECLINED, REOPENED, UPDATED;
    }

    Status getStatus();

    @OneToMany
    RepositoryActivityCommitMapping[] getCommits();

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
