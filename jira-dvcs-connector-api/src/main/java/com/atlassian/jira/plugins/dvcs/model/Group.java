package com.atlassian.jira.plugins.dvcs.model;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Used for handle DVCS groups.
 *
 */
public class Group
{
	private final String slug;
	
	public Group(String slug)
	{
		this.slug = slug;
	}

	public String getSlug()
	{
		return slug;
	}

	public boolean equals(Object obj) 
	{
        if (obj == null) return false;
        if (this == obj) return true;
        if (this.getClass() != obj.getClass()) return false;
        Group that = (Group) obj;
        return new EqualsBuilder().append(slug, that.slug).isEquals();
	};
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
}
