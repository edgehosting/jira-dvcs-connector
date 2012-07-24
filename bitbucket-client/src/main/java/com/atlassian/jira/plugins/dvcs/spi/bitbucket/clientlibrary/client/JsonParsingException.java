package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client;

/**
 * JsonParsingException
 *
 * @author Martin Skurla mskurla@atlassian.com
 */
public class JsonParsingException extends RuntimeException {

    public JsonParsingException(Throwable cause)
    {
        super(cause);
    }
}
