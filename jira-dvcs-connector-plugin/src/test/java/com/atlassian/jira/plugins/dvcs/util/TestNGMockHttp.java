package com.atlassian.jira.plugins.dvcs.util;

import com.atlassian.jira.web.ExecutingHttpRequest;
import org.mockito.Mockito;
import webwork.action.ActionContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.atlassian.jira.util.dbc.Assertions.notNull;
import static org.mockito.Mockito.reset;

/**
 * Adapted from MockHttp in jira core. Could not reuse it properly as all its constructors are private
 * and the starting and finished methods are protected.
 * @param <R>
 * @param <S>
 */
public class TestNGMockHttp<R extends HttpServletRequest, S extends HttpServletResponse>
{
    public static class MockitoTestNGMocks extends TestNGMockHttp<HttpServletRequest,HttpServletResponse>
    {
        private MockitoTestNGMocks(HttpServletRequest mockRequest, HttpServletResponse mockResponse)
        {
            super(mockRequest, mockResponse);
        }

        @Override
        public void beforeMethod() {
            reset(mockRequest());
            reset(mockResponse());
            super.beforeMethod();
        }

        @Override
        public void afterMethod() {
            super.afterMethod();    //To change body of overridden methods use File | Settings | File Templates.
        }
    }

    public static TestNGMockHttp withMockitoMocks()
    {
        return new MockitoTestNGMocks(Mockito.mock(HttpServletRequest.class), Mockito.mock(HttpServletResponse.class));
    }

    private final R mockRequest;
    private final S mockResponse;

    private TestNGMockHttp(R mockRequest, S mockResponse)
    {
        this.mockRequest = notNull("mockRequest", mockRequest);
        this.mockResponse = notNull("mockResponse", mockResponse);
    }

    public void beforeMethod()
    {
        ActionContext.setRequest(mockRequest);
        ActionContext.setResponse(mockResponse);
        ExecutingHttpRequest.set(mockRequest, mockResponse);
    }

    public void afterMethod()
    {
        ActionContext.setRequest(null);
        ActionContext.setResponse(null);
        ExecutingHttpRequest.clear();
    }

    public R mockRequest()
    {
        return mockRequest;
    }

    public S mockResponse()
    {
        return mockResponse;
    }
}