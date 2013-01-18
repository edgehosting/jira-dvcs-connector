package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.util.List;

public class BitbucketBranchesAndTags
{
    
    
/**
 * https://api.bitbucket.org/1.0/repositories/atlassian/jira-bitbucket-connector/branches-tags
 * 
 *  <pre>
 *  "branches": [
 *    {
 *        "changeset": "43b3aa22e47d01ce0087fbea9f6e047651595f0d",
 *       "heads": [
 *           "43b3aa22e47d01ce0087fbea9f6e047651595f0d"
 *       ],
 *       "name": "missing-commits-fix"
 *   },
 *   {
 *       "changeset": "f576b8122a70a593709d4c8e8c7e919380d80f93",
 *       "heads": [
 *           "f576b8122a70a593709d4c8e8c7e919380d80f93"
 *       ],
 *       "name": "jira4.x"
 *   },
 *   {
 *       "changeset": "aed7d0597288358a47f343de05cb0739dff2c4cb",
 *       "heads": [
 *           "aed7d0597288358a47f343de05cb0739dff2c4cb"
 *       ],
 *       "name": "github:enterprise"
 *   },
 *   {
 *       "changeset": "0a83294a586559853dcf939d20c9cca6cc59ce1b",
 *       "heads": [
 *           "0a83294a586559853dcf939d20c9cca6cc59ce1b"
 *       ],
 *       "name": "default"
 *   },
 */
    private List<BitbucketBranch> branches;
    private List<BitbucketTag> tags;
    
    public List<BitbucketBranch> getBranches()
    {
        return branches;
    }
    public void setBranches(List<BitbucketBranch> branches)
    {
        this.branches = branches;
    }
    public List<BitbucketTag> getTags()
    {
        return tags;
    }
    public void setTags(List<BitbucketTag> tags)
    {
        this.tags = tags;
    }
}
