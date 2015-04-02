package com.atlassian.jira.plugins.dvcs.rest.filter;

import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.google.common.base.Preconditions;
import com.sun.jersey.api.model.AbstractMethod;
import com.sun.jersey.spi.container.ResourceFilter;
import com.sun.jersey.spi.container.ResourceFilterFactory;

import java.util.Collections;
import java.util.List;
import javax.ws.rs.ext.Provider;

/**
 * <p>A {@link ResourceFilterFactory} that checks wether the client is authenticated or not.<p>
 * @see AdminOnlyResourceFilter
 */
@Provider
public class AdminOnlyResourceFilterFactory implements ResourceFilterFactory
{
    private final JiraAuthenticationContext authenticationContext;

    private final PermissionManager permissionManager;

    public AdminOnlyResourceFilterFactory(final JiraAuthenticationContext authenticationContext, final PermissionManager permissionManager)
    {
        this.authenticationContext = Preconditions.checkNotNull(authenticationContext);
        this.permissionManager = Preconditions.checkNotNull(permissionManager);
    }

    public List<ResourceFilter> create(AbstractMethod abstractMethod)
    {
        return Collections.<ResourceFilter>singletonList(new AdminOnlyResourceFilter(abstractMethod, authenticationContext, permissionManager));
    }
}
