package com.atlassian.jira.plugins.dvcs.event;

import net.java.ao.Entity;
import net.java.ao.Preload;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

import java.util.Date;
import javax.annotation.Nullable;

/**
 * AO mapping for a sync event.
 */
@Preload
@Table ("SYNC_EVENT")
public interface SyncEventMapping extends Entity
{
    String REPO_ID = "REPO_ID";
    String EVENT_DATE = "EVENT_DATE";
    String EVENT_CLASS = "EVENT_CLASS";
    String EVENT_JSON = "EVENT_JSON";
    String SCHEDULED_SYNC = "SCHEDULED_SYNC";

    /**
     * @return the id of the repository that the event is for
     */
    @NotNull
    @Indexed
    int getRepoId();
    void setRepoId(int repoId);

    /**
     * @return the event's date
     */
    @NotNull
    Date getEventDate();
    void setEventDate(Date eventDate);

    /**
     * @return the FQN of the event class
     */
    @NotNull
    @StringLength(StringLength.UNLIMITED)
    String getEventClass();
    void setEventClass(String eventClass);

    /**
     * @return an event serialised as JSON
     */
    @NotNull
    @StringLength(StringLength.UNLIMITED)
    String getEventJson();
    void setEventJson(String eventClass);

    /**
     * @return whether this event was raised during a scheduled sync
     */
    @Nullable
    Boolean getScheduledSync();
    void setScheduledSync(@Nullable Boolean scheduledSync);
}
