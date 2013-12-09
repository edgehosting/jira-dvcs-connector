package com.atlassian.jira.plugins.dvcs.ondemand;


public interface AccountsConfigService
{
    
    void scheduleReload();
    void reloadAsync();
    void reload();

}

