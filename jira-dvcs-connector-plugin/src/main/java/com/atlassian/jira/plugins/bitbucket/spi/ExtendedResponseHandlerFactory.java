package com.atlassian.jira.plugins.bitbucket.spi;

import com.atlassian.jira.plugins.bitbucket.api.impl.ExtendedResponseHandler;

public interface ExtendedResponseHandlerFactory
{
    public ExtendedResponseHandler create();
}