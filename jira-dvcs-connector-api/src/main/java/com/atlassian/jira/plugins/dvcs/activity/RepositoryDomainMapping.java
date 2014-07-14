package com.atlassian.jira.plugins.dvcs.activity;

import net.java.ao.Entity;
import net.java.ao.Polymorphic;
import net.java.ao.schema.Indexed;
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
     * The domain of the objects, mostly repository atm. For easy querying/deletion of objects by domain.
     * @return Domain of validity.
     */
    @NotNull
    @Indexed
    int getDomainId();

    /**
     * @param domainId
     *            {@link #getDomainId()}
     */
    void setDomainId(int domainId);

}
