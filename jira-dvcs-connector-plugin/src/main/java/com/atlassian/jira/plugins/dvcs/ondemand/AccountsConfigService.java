package com.atlassian.jira.plugins.dvcs.ondemand;

import com.atlassian.annotations.PublicApi;

@PublicApi
public interface AccountsConfigService
{
    void reload(boolean runAsync);
}

