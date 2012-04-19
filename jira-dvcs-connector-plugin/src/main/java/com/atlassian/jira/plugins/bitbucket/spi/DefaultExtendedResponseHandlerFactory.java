package com.atlassian.jira.plugins.bitbucket.spi;

import com.atlassian.jira.plugins.bitbucket.api.impl.ExtendedResponseHandler;


public class DefaultExtendedResponseHandlerFactory implements ExtendedResponseHandlerFactory
{
    @Override
    public ExtendedResponseHandler create()
    {
        return new ExtendedResponseHandler();
    }
}