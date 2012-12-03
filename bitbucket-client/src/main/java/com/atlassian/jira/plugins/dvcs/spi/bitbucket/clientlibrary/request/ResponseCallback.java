package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

public interface ResponseCallback<T>
{
    
    T onResponse(RemoteResponse response);

    public static final ResponseCallback<Void> EMPTY = new ResponseCallback<Void>()
    {
        @Override
        public Void onResponse(RemoteResponse response)
        {
            return null;
        }
        
    };
}

