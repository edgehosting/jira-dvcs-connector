package com.atlassian.jira.plugins.dvcs.rest.filter;

import com.atlassian.jira.compatibility.util.ApplicationUserUtil;
import com.atlassian.jira.plugins.dvcs.rest.security.AdminOnly;
import com.atlassian.jira.plugins.dvcs.rest.security.AuthorizationException;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
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
    private final PermissionManager permissionManager;

    public AdminOnlyResourceFilter(AbstractMethod abstractMethod, JiraAuthenticationContext authenticationContext, PermissionManager permissionManager)
    {
        this.abstractMethod = Preconditions.checkNotNull(abstractMethod);
        this.authenticationContext = Preconditions.checkNotNull(authenticationContext);
        this.permissionManager = Preconditions.checkNotNull(permissionManager);
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
            ApplicationUser user = authenticationContext.getUser();
            if  (user == null)
            {
                throw new AuthenticationRequiredException();
            }
            if( !isAdmin(user) )
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
        return permissionManager.hasPermission(Permissions.ADMINISTER, user);
    }
}
