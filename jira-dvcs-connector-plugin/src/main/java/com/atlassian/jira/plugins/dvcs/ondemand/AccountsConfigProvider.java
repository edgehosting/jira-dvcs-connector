package com.atlassian.jira.plugins.dvcs.ondemand;

import com.atlassian.annotations.PublicApi;

/**
 * The Interface AccountsConfigProvider.
 */
@PublicApi
public interface AccountsConfigProvider
{

    /**
     * Provide configuration.
     *
     * @return the accounts config if the config file is found
     * and can be read, otherwise <code>null</code>
     */
    AccountsConfig provideConfiguration();

    /**
     * Supports integrated accounts.
     *
     * @return true, if supports integrated accounts
     */
    boolean supportsIntegratedAccounts();
}

