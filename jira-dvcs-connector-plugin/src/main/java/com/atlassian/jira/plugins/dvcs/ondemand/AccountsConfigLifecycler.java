package com.atlassian.jira.plugins.dvcs.ondemand;

import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.sal.api.lifecycle.LifecycleAware;
import org.springframework.stereotype.Component;

@ExportAsService (LifecycleAware.class)
@Component
public class AccountsConfigLifecycler implements LifecycleAware
{
    private final AccountsConfigService configService;

    public AccountsConfigLifecycler(AccountsConfigService configService)
    {
        this.configService = configService;
    }

    @Override
    public void onStart()
    {
        configService.scheduleReload();
    }

}

