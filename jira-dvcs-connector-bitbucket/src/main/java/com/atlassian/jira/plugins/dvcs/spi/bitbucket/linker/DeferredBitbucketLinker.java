package com.atlassian.jira.plugins.dvcs.spi.bitbucket.linker;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.util.DvcsConstants;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.util.concurrent.ThreadFactories;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DeferredBitbucketLinker implements BitbucketLinker, DisposableBean
{
    private final Logger log = LoggerFactory.getLogger(DeferredBitbucketLinker.class);

	private final BitbucketLinker bitbucketLinker;
	private final ThreadPoolExecutor executor;

    private final PluginSettingsFactory pluginSettingsFactory;

    public DeferredBitbucketLinker(@Qualifier("bitbucketLinker") BitbucketLinker bitbucketLinker,
            PluginSettingsFactory pluginSettingsFactory)
    {

		this.bitbucketLinker = bitbucketLinker;
        this.pluginSettingsFactory = pluginSettingsFactory;
		// would be nice to have 2-3 threads but that doesn't seem to be trivial task: 
		// http://stackoverflow.com/questions/3419380/threadpoolexecutor-policy
		executor = new ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(),
		        ThreadFactories.namedThreadFactory("BitbucketLinkerThread"));
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() throws Exception
    {
        executor.shutdown();
        if (!executor.awaitTermination(1, TimeUnit.MINUTES))
        {
            log.error("Unable properly shutdown queued tasks.");
        }
    }

	@Override
    public void linkRepository(final Repository repository, final Set<String> projectKeys)
    {
        addTaskAtTheEndOfQueue(new BitbucketLinkingTask(repository)
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
	    addTaskAtTheEndOfQueue(new BitbucketLinkingTask(repository)
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
        addTaskAtTheEndOfQueue(new BitbucketLinkingTask(repository)
        {
            @Override
            public void run()
            {
                bitbucketLinker.linkRepositoryIncremental(repository, projectKeys);
            }
        });
	}

	private void addTaskAtTheEndOfQueue(Runnable task)
    {
        if (!isLinkersEnabled())
        {
            log.debug("Linkers disabled.");
            return;
        }
        executor.remove(task);
        executor.execute(task);
        log.debug("QUEUED:" + task);
    }
	
    private boolean isLinkersEnabled()
    {
        String setting = (String) pluginSettingsFactory.createGlobalSettings().get(DvcsConstants.LINKERS_ENABLED_SETTINGS_PARAM);
        if (StringUtils.isNotBlank(setting))
        {
            return BooleanUtils.toBoolean(setting);
        } else
        {
            return true;
        }
    }

	private abstract class BitbucketLinkingTask implements Runnable
    {
	    private final Repository repository;
		
		private BitbucketLinkingTask(Repository repository)
        {
			this.repository = repository;
        }

		@Override
		public abstract void run();
		
		@Override
		public boolean equals(Object obj)
		{
			if (obj == null) return false;
			if (this==obj) return true;
			if (this.getClass()!=obj.getClass()) return false;
			BitbucketLinkingTask that = (BitbucketLinkingTask) obj;

			return new EqualsBuilder()
					.append(repository.getRepositoryUrl(), that.repository.getRepositoryUrl())
					.isEquals();
		}
		
		@Override
		public int hashCode()
		{
	        return new HashCodeBuilder(17, 37)
            	.append(repository.getRepositoryUrl())
            	.toHashCode();
		}
		
		@Override
		public String toString()
		{
			return "Configuring links on " + repository.getRepositoryUrl();
		}
    }
}
