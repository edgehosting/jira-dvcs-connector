package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

/**
 * BitbucketDiffstat
 * 
 * <pre>
 * 
 *    "diffstat": {
 *       "removed": 2, 
 *       "added": 2
 *   }
 * 
 * </pre>
 *
 * @author Martin Skurla mskurla@atlassian.com
 */
public class BitbucketDiffstat {

    private Integer added;
    private Integer removed;

    
    public Integer getAdded()
    {
        return added;
    }

    public void setAdded(Integer added)
    {
        this.added = added;
    }

    public Integer getRemoved()
    {
        return removed;
    }

    public void setRemoved(Integer removed)
    {
        this.removed = removed;
    }
}
