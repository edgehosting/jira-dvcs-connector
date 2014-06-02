package com.atlassian.jira.plugins.dvcs;

import com.atlassian.beehive.ClusterLockService;
import com.atlassian.beehive.SimpleClusterLockService;
import com.atlassian.beehive.compat.ClusterLockServiceFactory;
import org.apache.commons.lang3.Validate;

/**
 * A ClusterLockServiceFactory that always returns a given ClusterLockService.
 */
public class DumbClusterLockServiceFactory extends ClusterLockServiceFactory
{
    private final ClusterLockService service;

    /**
     * Constructor to use when the test wants to use a SimpleClusterLockService.
     */
    public DumbClusterLockServiceFactory()
    {
        this(new SimpleClusterLockService());
    }

    /**
     * Constructor to use when the test wants to provide the ClusterLockService, e.g. a mock.
     *
     * @param service the service that #getClusterLockService should return (required)
     */
    public DumbClusterLockServiceFactory(final ClusterLockService service)
    {
        Validate.notNull(service);
        this.service = service;
    }

    @Override
    public ClusterLockService getClusterLockService()
    {
        return service;
    }
}
