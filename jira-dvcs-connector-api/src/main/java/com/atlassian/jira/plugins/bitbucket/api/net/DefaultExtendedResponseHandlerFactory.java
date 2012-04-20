package com.atlassian.jira.plugins.bitbucket.api.net;



public class DefaultExtendedResponseHandlerFactory implements ExtendedResponseHandlerFactory
{
    @Override
    public ExtendedResponseHandler create()
    {
        return new ExtendedResponseHandler();
    }
}