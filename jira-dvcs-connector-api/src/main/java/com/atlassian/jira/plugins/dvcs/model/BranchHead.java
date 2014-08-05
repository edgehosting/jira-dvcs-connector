package com.atlassian.jira.plugins.dvcs.model;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.codehaus.jackson.annotate.JsonCreator;

public class BranchHead
{
    private String branchName;
    private String head;

    @JsonCreator
    private BranchHead() {}

    public BranchHead(String name, String node)
    {
        this.branchName = name;
        this.head = node;
    }

    public String getName()
    {
        return branchName;
    }

    public String getHead()
    {
        return head;
    }

    public void setName(String branchName)
    {
        this.branchName = branchName;
    }

    public void setHead(String node)
    {
        this.head = node;
    }
    
    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BranchHead that = (BranchHead) o;

        return new EqualsBuilder()
                .append(branchName, that.branchName)
                .append(head, that.head)
                .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder()
                .append(branchName)
                .append(head)
                .hashCode();
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("branchName", branchName)
                .append("head", head)
                .toString();
    }
}
