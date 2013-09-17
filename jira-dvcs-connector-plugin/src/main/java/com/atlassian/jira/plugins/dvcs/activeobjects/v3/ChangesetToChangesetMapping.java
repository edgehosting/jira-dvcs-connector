package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import net.java.ao.Entity;
import net.java.ao.schema.Table;

/**
 * Represents relation of {@link ChangesetMapping}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
@Table("CHANGESET_PARENT")
public interface ChangesetToChangesetMapping extends Entity
{

    /**
     * @return Relation owner
     */
    ChangesetMapping getChangeset();

    /**
     * @return {@link ChangesetMapping#getParents()}
     */
    ChangesetMapping getParent();

}
