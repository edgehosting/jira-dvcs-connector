package com.atlassian.jira.plugins.dvcs.model;

import org.codehaus.jackson.annotate.JsonCreator;

public class Participant
{
    
    public static final String ROLE_PARTICIPANT = "PARTICIPANT";
    public static final String ROLE_REVIEWER = "REVIEWER";
    
    private String username;
    private boolean approved;
    private String role;

    @JsonCreator
    private Participant() {}

    public Participant(final String username, final boolean approved, final String role)
    {
        this.approved = approved;
        this.username = username;
        this.role = role;
    }

    public boolean isApproved()
    {
        return approved;
    }

    public void setApproved(final boolean approved)
    {
        this.approved = approved;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(final String username)
    {
        this.username = username;
    }

    public String getRole()
    {
        return role;
    }

    public void setRole(final String role)
    {
        this.role = role;
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        final Participant participant = (Participant) o;

        if (!username.equals(participant.username)) { return false; }

        return true;
    }

    @Override
    public int hashCode()
    {
        return username.hashCode();
    }
}
