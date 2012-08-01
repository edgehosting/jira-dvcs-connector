package com.atlassian.jira.plugins.dvcs.ondemand;

public interface AccountsConfigProvider
{

    AccountsConfig provideConfiguration();

    boolean supportsIntegratedAccounts();
}

