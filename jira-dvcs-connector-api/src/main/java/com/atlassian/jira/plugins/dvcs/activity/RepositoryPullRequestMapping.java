package com.atlassian.jira.plugins.dvcs.activity;

import net.java.ao.schema.Table;

@Table("PULL_REQUEST")
public interface RepositoryPullRequestMapping extends RepositoryDomainMapping
{
    String REMOTE_ID = "REMOTE_ID";
    String TO_REPO_ID = "TO_REPOSITORY_ID";
    String NAME = "NAME";
    String DESCRIPTION = "DESCRIPTION";
    String URL = "URL";
    String SOURCE_URL = "SOURCE_URL";

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

    String getSourceUrl();

    //
    // setters
    //
    void setRemoteId(Long id);

    void setToRepoId(int repoId);

    void setName(String name);

    void setDescription(String description);

    void setUrl(String url);

    void setSourceUrl(String sourceUrl);
}
