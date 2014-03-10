package com.atlassian.jira.plugins.dvcs.ondemand;

import com.atlassian.scheduler.compat.JobHandler;
import com.atlassian.scheduler.compat.JobInfo;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * Runs the scheduled job of reloading Bitbucket accounts.
 */
@Component
public class BitbucketAccountsReloadJobHandler implements ApplicationContextAware, JobHandler
{
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException
    {
        this.applicationContext = applicationContext;
    }

    @Override
    public void execute(final JobInfo jobInfo)
    {
        getConfigService().reload();
    }

    private BitbucketAccountsConfigService getConfigService()
    {
        // Not injecting this via DI because it gives intermittent circular dependencies
        @SuppressWarnings("unchecked")
        final Collection<BitbucketAccountsConfigService> services =
                applicationContext.getBeansOfType(BitbucketAccountsConfigService.class).values();
        Validate.isTrue(services.size() == 1, "Expected one service but found %d: %s", services.size(), services);
        return services.iterator().next();
    }
}
