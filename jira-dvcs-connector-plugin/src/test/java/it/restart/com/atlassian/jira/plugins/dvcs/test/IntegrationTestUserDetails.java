package it.restart.com.atlassian.jira.plugins.dvcs.test;

import com.atlassian.jira.plugins.dvcs.util.PasswordUtil;
import it.util.TestAccounts;

import static it.util.TestAccounts.FIRST_ACCOUNT;

/**
 * Contains the Basic information for the test account that is re-used across our tests.
 */
public final class IntegrationTestUserDetails
{
    public static final String ACCOUNT_NAME = TestAccounts.FIRST_ACCOUNT;

    public static final String PASSWORD = PasswordUtil.getPassword(FIRST_ACCOUNT);
}
