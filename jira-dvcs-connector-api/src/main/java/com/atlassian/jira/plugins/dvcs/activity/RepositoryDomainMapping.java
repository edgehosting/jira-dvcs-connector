package com.atlassian.jira.plugins.dvcs.activity;

import net.java.ao.Entity;
import net.java.ao.Polymorphic;
import net.java.ao.schema.NotNull;

/**
 * Base of all entities. Defines domain of object validity.
 * 
 * @author Stanislav Dvorscak
 * 
 */
@Polymorphic
public interface RepositoryDomainMapping extends Entity
{

    /**
     * @see #getDomainId()
     */
    String DOMAIN = "DOMAIN_ID";

    /**
     * @return Domain of validity.
     */
    @NotNull
    int getDomainId();

    /**
     * @param domainId
     *            {@link #getDomainId()}
     */
    void setDomainId(int domainId);

}
