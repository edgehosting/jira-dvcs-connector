package com.atlassian.jira.plugins.dvcs.spi.bitbucket.linker;

import com.atlassian.beehive.ClusterLockService;
import com.atlassian.beehive.compat.ClusterLockServiceFactory;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.locks.Lock;

import static com.atlassian.jira.plugins.dvcs.util.DvcsConstants.LINKERS_ENABLED_SETTINGS_PARAM;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang.StringUtils.isBlank;

@Component("deferredBitbucketLinker")
public class DeferredBitbucketLinker implements BitbucketLinker
{
    /**
     * Returns the name of the cluster-wide lock to acquire before modifying the links for the given repository.
     *
     * @param repository the repository whose links are being modified
     * @return a globally unique lock name
     */
    @VisibleForTesting
    static String getLockName(final Repository repository)
    {
        return DeferredBitbucketLinker.class.getName() + "." + repository.getRepositoryUrl();
    }

    private final Logger log = LoggerFactory.getLogger(DeferredBitbucketLinker.class);
    private final BitbucketLinker bitbucketLinker;
    private final ClusterLockService clusterLockService;
    private final PluginSettingsFactory pluginSettingsFactory;

    @Autowired
    public DeferredBitbucketLinker(@Qualifier ("bitbucketLinker") final BitbucketLinker bitbucketLinker,
            final ClusterLockServiceFactory clusterLockServiceFactory,
            @ComponentImport final PluginSettingsFactory pluginSettingsFactory)
    {
        this.bitbucketLinker = bitbucketLinker;
        this.clusterLockService = clusterLockServiceFactory.getClusterLockService();
        this.pluginSettingsFactory = checkNotNull(pluginSettingsFactory);
    }

    @Override
    public void linkRepository(final Repository repository, final Set<String> projectKeys)
    {
        configureLinks(repository, new Runnable()
        {
            @Override
            public void run()
            {
                bitbucketLinker.linkRepository(repository, projectKeys);
            }
        });
    }

    @Override
    public void unlinkRepository(final Repository repository)
    {
        configureLinks(repository, new Runnable()
        {
            @Override
            public void run()
            {
                bitbucketLinker.unlinkRepository(repository);
            }
        });
    }

    @Override
    public void linkRepositoryIncremental(final Repository repository, final Set<String> projectKeys)
    {
        configureLinks(repository, new Runnable()
        {
            @Override
            public void run()
            {
                bitbucketLinker.linkRepositoryIncremental(repository, projectKeys);
            }
        });
    }

    private void configureLinks(final Repository repository, final Runnable task)
    {
        if (!isLinkersEnabled())
        {
            log.debug("Linkers disabled.");
            return;
        }
        final Lock lock = clusterLockService.getLockForName(getLockName(repository));
        lock.lock();
        try
        {
            task.run();
            log.debug("Ran: Configuring links on " + repository.getRepositoryUrl());
        }
        finally
        {
            lock.unlock();
        }
    }

    private boolean isLinkersEnabled()
    {
        final String setting = (String) pluginSettingsFactory.createGlobalSettings().get(LINKERS_ENABLED_SETTINGS_PARAM);
        return isBlank(setting) || BooleanUtils.toBoolean(setting);
    }
}
