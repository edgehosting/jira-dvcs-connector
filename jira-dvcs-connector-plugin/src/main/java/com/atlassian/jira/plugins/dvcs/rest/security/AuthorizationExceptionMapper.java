package com.atlassian.jira.plugins.dvcs.rest.security;

import com.atlassian.plugins.rest.common.Status;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * <p>Exception mapper that takes care of {@link SecurityException security exceptions}</p>
 * @since 1.0
 */
@Provider
public class AuthorizationExceptionMapper implements ExceptionMapper<AuthorizationException>
{
    @Context
    Request request;

    public Response toResponse(AuthorizationException exception)
    {
        return Status.forbidden().message(exception.getMessage()).responseBuilder().type(Status.variantFor(request)).build();
    }
}
