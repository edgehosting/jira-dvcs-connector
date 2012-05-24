package com.atlassian.jira.plugins.bitbucket.spi.bitbucket.clientlibrary;

import java.io.IOException;

public class BitbucketClientException extends IOException
{

    public BitbucketClientException(String string, IOException e)
    {
        super(string, e);
    }

}
