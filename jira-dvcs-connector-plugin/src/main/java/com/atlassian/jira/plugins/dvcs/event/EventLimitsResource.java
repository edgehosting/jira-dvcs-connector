package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.jira.plugins.dvcs.rest.security.AdminOnly;
import com.atlassian.jira.plugins.dvcs.sync.SyncConfig;
import com.google.common.collect.Maps;

import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Allows admins to set/get the event limits.
 */
@AdminOnly
@Path ("event/limits")
@Consumes (MediaType.APPLICATION_JSON)
@Produces (MediaType.APPLICATION_JSON)
public class EventLimitsResource
{
    private final SyncConfig syncConfig;

    public EventLimitsResource(SyncConfig syncConfig)
    {
        this.syncConfig = syncConfig;
    }

    /**
     * Returns the limits that are currently in effect.
     *
     * @return the limits that are currently in effect.
     */
    @GET
    public Map<String, Integer> getAll()
    {
        final Map<String, Integer> limits = Maps.newHashMap();
        for (EventLimit limit : EventLimit.values())
        {
            limits.put(limit.name(), syncConfig.getEffectiveLimit(limit));
        }

        return limits;
    }

    /**
     * Makes the provided limits the new effective limits.
     *
     * @param newLimits the limits to change
     * @return the limits currently in effect
     */
    @PUT
    public Map<String, Integer> put(Map<String, Integer> newLimits)
    {
        Map<EventLimit, Integer> limitsToSet = validateNewLimits(newLimits);

        // update all the limits
        for (EventLimit limitType : limitsToSet.keySet())
        {
            syncConfig.setEffectiveLimit(limitType, limitsToSet.get(limitType));
        }

        // finally, return the current effective limits
        return getAll();
    }

    private Map<EventLimit, Integer> validateNewLimits(Map<String, Integer> newLimits)
    {
        final Map<EventLimit, Integer> limitsToUpdate = Maps.newHashMap();
        for (String name : newLimits.keySet())
        {
            try
            {
                limitsToUpdate.put(EventLimit.valueOf(name), newLimits.get(name));
            }
            catch (IllegalArgumentException e)
            {
                throw new WebApplicationException(Response.status(400).entity(String.format("No such limit: %s", name)).build());
            }
        }

        return limitsToUpdate;
    }
}
