package com.atlassian.jira.plugins.bitbucket.spi;


public class DefaultExtendedResponseHandlerFactory implements ExtendedResponseHandlerFactory
{
    @Override
    public ExtendedResponseHandler create()
    {
        return new ExtendedResponseHandler();
    }
}