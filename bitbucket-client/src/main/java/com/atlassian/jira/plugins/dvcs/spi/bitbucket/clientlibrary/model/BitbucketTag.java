package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;


/**
 * 
 * <pre>
 * {
 *   "changeset": "43b3aa22e47d01ce0087fbea9f6e047651595f0d",
 *    "name": "missing-commits-fix"
 * }
 * </pre>
 *
 */
public class BitbucketTag
{
    private String changeset;
    private String name;
    
    public String getChangeset()
    {
        return changeset;
    }
    public void setChangeset(String changeset)
    {
        this.changeset = changeset;
    }
    public String getName()
    {
        return name;
    }
    public void setName(String name)
    {
        this.name = name;
    }

}
