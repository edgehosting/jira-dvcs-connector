package com.atlassian.jira.plugins.dvcs.ondemand;

import com.atlassian.sal.api.lifecycle.LifecycleAware;

public interface AccountsConfigService
{

    /**
     * Schedules single relaod, because AO is not available in {@link LifecycleAware#onStart()}.
     */
    void scheduleReload();

    /**
     * Returns immediately, and reloads accounts configuration asynchronously (in separate thread).
     * 
     * @see #reload()
     */
    void reloadAsync();

    /**
     * Reloads accounts configuration, and returns when it is done.
     * 
     * @see #reloadAsync()
     */
    void reload();

}
