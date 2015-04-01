package com.atlassian.jira.plugins.dvcs.rest.filter;

import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.plugins.dvcs.rest.security.AdminOnly;
import com.atlassian.jira.plugins.dvcs.rest.security.AuthorizationException;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugins.rest.common.security.AuthenticationRequiredException;
import com.google.common.base.Preconditions;
import com.sun.jersey.api.model.AbstractMethod;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;

import javax.ws.rs.ext.Provider;

/**
 * <p>This is a Jersey resource filter that, if the resource is marked by {@link AdminOnly} annotation,
 * checks weather the current client is authenticated and it is admin user
 * If the client is not authenticated then an {@link AuthenticationRequiredException} is thrown.
 * If the client is not admin user then an {@link AuthorizationException} is thrown</p>
 */
@Provider
public class AdminOnlyResourceFilter implements ResourceFilter, ContainerRequestFilter
{
    private final AbstractMethod abstractMethod;
    private final JiraAuthenticationContext authenticationContext;
    private final GlobalPermissionManager globalPermissionManager;

    public AdminOnlyResourceFilter(AbstractMethod abstractMethod, JiraAuthenticationContext authenticationContext, GlobalPermissionManager globalPermissionManager)
    {
        this.abstractMethod = Preconditions.checkNotNull(abstractMethod);
        this.authenticationContext = Preconditions.checkNotNull(authenticationContext);
        this.globalPermissionManager = Preconditions.checkNotNull(globalPermissionManager);
    }

    public ContainerRequestFilter getRequestFilter()
    {
        return this;
    }

    public ContainerResponseFilter getResponseFilter()
    {
        return null;
    }

    public ContainerRequest filter(ContainerRequest request)
    {
        if ( isAdminNeeded() )
        {
        	ApplicationUser loggedInUser = authenticationContext.getLoggedInUser();
        	if  (loggedInUser == null)
        	{
        		throw new AuthenticationRequiredException();
        	}
        	if( !isAdmin(loggedInUser) )
        	{
        		throw new AuthorizationException();
        	}
        }
        return request;
    }

    private boolean isAdminNeeded()
    {
        return (abstractMethod.getMethod() != null && abstractMethod.getMethod().getAnnotation(AdminOnly.class) != null)
                || abstractMethod.getResource().getAnnotation(AdminOnly.class) != null;
    }

    private boolean isAdmin(ApplicationUser user)
    {
        return globalPermissionManager.hasPermission(GlobalPermissionKey.ADMINISTER, user);
    }
}
