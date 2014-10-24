package com.atlassian.jira.plugins.dvcs.rest;

import java.util.HashSet;
import java.util.Set;

public class ChangesetHash
{
    private final Set<String> parents = new HashSet<String>();
    private final Set<String> children = new HashSet<String>();
    private final String hash;

    public ChangesetHash(final String hash)
    {
        this.hash = hash;
    }

    public Set<String> getParents()
    {
        return parents;
    }

    public boolean addParent(final String parentHash)
    {
        return parents.add(parentHash);
    }

    public Set<String> getChildren()
    {
        return children;
    }

    public boolean addChild(final String childHash)
    {
        return children.add(childHash);
    }
}
