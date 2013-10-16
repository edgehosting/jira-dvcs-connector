package com.atlassian.jira.plugins.dvcs.activity;

import java.util.Date;

import net.java.ao.ManyToMany;
import net.java.ao.schema.Table;

@Table("PULL_REQUEST")
public interface RepositoryPullRequestMapping extends RepositoryDomainMapping
{
    String REMOTE_ID = "REMOTE_ID";
    String TO_REPO_ID = "TO_REPOSITORY_ID";
    String NAME = "NAME";
    // this is not used
    String DESCRIPTION = "DESCRIPTION";
    String URL = "URL";

    String AUTHOR = "AUTHOR";
    String SOURCE_REPO = "SOURCE_REPO";
    String SOURCE_BRANCH = "SOURCE_BRANCH";
    String DESTINATION_BRANCH = "DESTINATION_BRANCH";
    String LAST_STATUS = "LAST_STATUS";
    String CREATED_ON = "CREATED_ON";
    String UPDATED_ON = "UPDATED_ON";

    public enum Status {
        OPEN("open"), DECLINED("rejected"), MERGED("fulfilled");
        private String bbString;
        private Status(String bbString)
        {
            this.bbString = bbString;
        }
        public static Status fromBbString(String string)
        {
            Status[] values = values();
            for (Status status : values)
            {
                if (status.bbString.equalsIgnoreCase(string))
                {
                    return status;
                }
            }
            return OPEN;
        }
    }

    //
    // getters
    //
    /**
     * @return remote Id of this pull request
     */
    Long getRemoteId();

    /**
     * @return local id of destination repository
     */
    int getToRepositoryId();

    String getName();

    String getDescription();

    String getUrl();

    String getSourceBranch();

    String getDestinationBranch();

    String getLastStatus();

    Date getCreatedOn();

    Date getUpdatedOn();

    String getAuthor();

    @ManyToMany(RepositoryPullRequestToCommitMapping.class)
    RepositoryCommitMapping[] getCommits();

    String getSourceRepo();

    //
    // setters
    //
    void setRemoteId(Long id);

    void setToRepoId(int repoId);

    void setName(String name);

    void setDescription(String description);

    void setUrl(String url);

    void setSourceBranch(String branch);

    void setDestinationBranch(String branch);

    void setLastStatus(String status);

    void setCreatedOn(Date date);

    void setUpdatedOn(Date date);

    void setAuthor(String author);

    void setSourceRepo(String sourceRepo);
}
