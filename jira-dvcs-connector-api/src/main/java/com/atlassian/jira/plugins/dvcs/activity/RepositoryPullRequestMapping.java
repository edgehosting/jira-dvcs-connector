package com.atlassian.jira.plugins.dvcs.activity;

import net.java.ao.ManyToMany;
import net.java.ao.OneToMany;
import net.java.ao.Preload;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.Table;

import java.util.Date;

@Preload
@Table("PULL_REQUEST")
public interface RepositoryPullRequestMapping extends RepositoryDomainMapping
{
    String REMOTE_ID = "REMOTE_ID";
    String TO_REPO_ID = "TO_REPOSITORY_ID";
    String NAME = "NAME";
    String URL = "URL";

    String AUTHOR = "AUTHOR";
    String SOURCE_REPO = "SOURCE_REPO";
    String SOURCE_BRANCH = "SOURCE_BRANCH";
    String DESTINATION_BRANCH = "DESTINATION_BRANCH";
    String LAST_STATUS = "LAST_STATUS";
    String CREATED_ON = "CREATED_ON";
    String UPDATED_ON = "UPDATED_ON";
    String PARTICIPANTS = "PARTICIPANTS";
    String COMMENT_COUNT = "COMMENT_COUNT";
    String EXECUTED_BY = "EXECUTED_BY";

    //
    // getters
    //
    /**
     * Unique per repo on Bitbucket and unique globally on GitHub.
     *
     * @return remote Id of this pull request
     */
    @Indexed
    Long getRemoteId();

    /**
     * @return local id of destination repository
     */
    @Indexed
    int getToRepositoryId();

    String getName();

    String getUrl();

    String getSourceBranch();

    String getDestinationBranch();

    String getLastStatus();

    Date getCreatedOn();

    Date getUpdatedOn();

    String getAuthor();

    @ManyToMany(reverse = "getRequest", through = "getCommit", value = RepositoryPullRequestToCommitMapping.class)
    RepositoryCommitMapping[] getCommits();

    String getSourceRepo();

    @OneToMany (reverse = "getPullRequest")
    PullRequestParticipantMapping[] getParticipants();

    int getCommentCount();

    /**
     * Pull requests can be declined, merged or reopened by some user that is not the author.
     * In Github/Githube for PR declined and reopened the user will be null because it would be expensive to find out
     * the actor.
     *
     * @return actor who created, merged, declined or reopened the pull request.
     *
     */
    String getExecutedBy();

    //
    // setters
    //
    void setRemoteId(Long id);

    void setToRepositoryId(int repoId);

    void setName(String name);

    void setUrl(String url);

    void setSourceBranch(String branch);

    void setDestinationBranch(String branch);

    void setLastStatus(String status);

    void setCreatedOn(Date date);

    void setUpdatedOn(Date date);

    void setAuthor(String author);

    void setSourceRepo(String sourceRepo);

    void setCommentCount(int commentCount);

    void setExecutedBy(String user);
}
