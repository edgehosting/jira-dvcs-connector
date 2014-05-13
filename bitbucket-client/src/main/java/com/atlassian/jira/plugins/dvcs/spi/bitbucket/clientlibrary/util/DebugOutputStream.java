package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.util;

import org.slf4j.Logger;

import java.io.IOException;
import java.io.OutputStream;

public class DebugOutputStream extends OutputStream
{
    private final Logger delegate;

    public DebugOutputStream(Logger delegate)
    {
        this.delegate = delegate;
    }

    @Override
    public void write(int b) throws IOException
    {
        // does nothing
    }
    
    @Override
    public void write(byte[] b) throws IOException
    {
        delegate.debug(new String(b, "utf-8"));
    }

}

