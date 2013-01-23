package com.atlassian.jira.plugins.dvcs.rest.security;

import com.atlassian.jira.plugins.dvcs.rest.filter.AdminOnlyResourceFilter;



/**
 * Exception thrown by the {@link AdminOnlyResourceFilter} to indicate a user is not a authorized.
 *
 * @since 1.1
 */
public class AuthorizationException extends SecurityException
{
    public AuthorizationException()
    {
        super("Client is not authorized to access this resource.");
    }
}
