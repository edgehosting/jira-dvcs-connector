package com.atlassian.jira.plugins.dvcs.spi.bitbucket.activeobjects;

import net.java.ao.Entity;
import net.java.ao.schema.Table;

@Table("BITBUCKET_PR_COMMITS")
public interface BitbucketPullRequestCommitMapping extends Entity
{
    String LOCAL_ID = "LOCAL_ID";
    String PULL_REQUEST_ID = "PULL_REQUEST_ID";
    String NODE = "NODE";
    String NEXT_NODE = "NEXT_NODE";
    
    int getLocalId();
    int getPullRequestId();
    String getNode();
    String getNextNode();
    
    void setLocalId(int id);
    void setPullRequestId(int pullRequestId);
    void setNode(String node);
    void setNextNode(String nextNode);
}
