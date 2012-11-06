package com.atlassian.jira.plugins.dvcs.spi.bitbucket.linker;

import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.util.concurrent.ThreadFactories;

public class DeferredBitbucketLinker implements BitbucketLinker
{
    private final Logger log = LoggerFactory.getLogger(DeferredBitbucketLinker.class);

	private final BitbucketLinker bitbucketLinker;
	private final ThreadPoolExecutor executor;

	public DeferredBitbucketLinker(@Qualifier("bitbucketLinker") BitbucketLinker bitbucketLinker)
    {

		this.bitbucketLinker = bitbucketLinker;
		// would be nice to have 2-3 threads but that doesn't seem to be trivial task: 
		// http://stackoverflow.com/questions/3419380/threadpoolexecutor-policy
		executor = new ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(),
		        ThreadFactories.namedThreadFactory("BitbucketLinkerThread"));
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
	    executor.remove(task);
		executor.execute(task);
		log.debug("QUEUED:" + task);
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
