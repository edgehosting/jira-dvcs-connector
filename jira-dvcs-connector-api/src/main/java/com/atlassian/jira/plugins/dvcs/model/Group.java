package com.atlassian.jira.plugins.dvcs.model;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.io.Serializable;

/**
 * Used for handle DVCS groups.
 */
public class Group implements Serializable
{
    private final String slug;
    private final String niceName;

    public Group(String slug)
    {
        this.slug = slug;
        this.niceName = null;
    }

    public Group(String slug, String niceName)
    {
        this.slug = slug;
        this.niceName = niceName;
    }

    public String getSlug()
    {
        return slug;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null) { return false; }
        if (this == obj) { return true; }
        if (this.getClass() != obj.getClass()) { return false; }
        Group that = (Group) obj;
        return new EqualsBuilder().append(slug, that.slug).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37).append(slug).toHashCode();
    }

    @Override
    public String toString()
    {
        return slug;
    }

    public String getNiceName()
    {
        return niceName;
    }
}
