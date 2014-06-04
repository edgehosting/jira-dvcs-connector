package it.com.atlassian.jira.plugins.dvcs.event;

import com.sun.jersey.api.client.ClientResponse;

import java.util.Map;

/**
 * Response from the Limits resource.
 */
public interface LimitsResponse
{
    /**
     * @return the response status
     */
    int status();

    /**
     * @return a Map containing the limits
     * @throws IllegalStateException if {@link #status()} is not 200
     */
    Map<String, Integer> limits();

    /**
     * Returns the Jersey ClientResponse.
     *
     * @return a ClientResponse
     */
    ClientResponse clientResponse();
}
