package com.atlassian.jira.plugins.dvcs.ondemand;

import com.atlassian.jira.plugins.dvcs.scheduler.SchedulerLauncher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import static com.atlassian.jira.plugins.dvcs.scheduler.SchedulerLauncher.SchedulerLauncherJob;

@Component
public class AccountsConfigLifecycler
{
    private static final Logger log = LoggerFactory.getLogger(AccountsConfigLifecycler.class);

    private final AccountsConfigService configService;
    private final SchedulerLauncher schedulerLauncher;

    @Autowired
    public AccountsConfigLifecycler(AccountsConfigService configService, final SchedulerLauncher schedulerLauncher)
    {
        this.configService = configService;
        this.schedulerLauncher = schedulerLauncher;
    }

    @PostConstruct
    public void postConstruct()
    {
        schedulerLauncher.runWhenReady(new SchedulerLauncherJob()
        {
            @Override
            public void run()
            {
                configService.scheduleReload();
                log.debug("executed launcher job");
            }
        });
        log.debug("scheduled launcher job");
    }
}

