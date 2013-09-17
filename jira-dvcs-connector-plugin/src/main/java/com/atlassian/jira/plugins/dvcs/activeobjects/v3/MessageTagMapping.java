package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import net.java.ao.Entity;
import net.java.ao.schema.Table;

/**
 * Relation mapping of {@link MessageMapping#getTags()}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
@Table("MESSAGE_TAG")
public interface MessageTagMapping extends Entity
{

    /**
     * @see #getTag()
     */
    String TAG = "TAG";

    /**
     * @see #getMessage()
     */
    String MESSAGE = "MESSAGE_ID";

    /**
     * @return tag of message
     */
    String getTag();

    /**
     * @return message owner of this tag
     */
    MessageMapping getMessage();

}
