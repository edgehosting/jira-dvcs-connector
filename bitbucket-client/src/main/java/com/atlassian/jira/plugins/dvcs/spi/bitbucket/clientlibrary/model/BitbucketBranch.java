package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;
import java.util.List;

/**
 *
 * <pre>
 * {
 *   "changeset": "43b3aa22e47d01ce0087fbea9f6e047651595f0d",
 *   "heads": [
 *     "43b3aa22e47d01ce0087fbea9f6e047651595f0d"
 *    ],
 *    "name": "missing-commits-fix"
 * }
 * </pre>
 */
public class BitbucketBranch implements Serializable
{
    private static final long serialVersionUID = 980662238990762018L;

    private String changeset;
    private List<String> heads;
    private String name;
    private boolean mainbranch;

    public BitbucketBranch()
    {
    }

    public BitbucketBranch(final String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }
    public void setName(String name)
    {
        this.name = name;
    }
    public String getChangeset()
    {
        return changeset;
    }
    public void setChangeset(String changeset)
    {
        this.changeset = changeset;
    }
    public List<String> getHeads()
    {
        return heads;
    }
    public void setHeads(List<String> heads)
    {
        this.heads = heads;
    }
    public boolean isMainbranch()
    {
        return mainbranch;
    }
    public void setMainbranch(boolean mainbranch)
    {
        this.mainbranch = mainbranch;
    }
}
