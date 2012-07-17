package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary;

import java.io.Serializable;

/** 
 * Represents RepositoryLinks from
 *      http://confluence.atlassian.com/display/BITBUCKET/Repository+links
 */
public class RepositoryLink implements Serializable
{
    public class Handler
    {
        private String url;
        private String name;
        private String key;
        
        public String getUrl()
        {
            return url;
        }
        public String getName()
        {
            return name;
        }
        public String getKey()
        {
            return key;
        }
    }

    public static final String TYPE_JIRA = "jira";
    
    int id;
    private Handler handler;
    
    public int getId()
    {
        return id;
    }

    public Handler getHandler()
    {
        return handler;
    }
    
    @Override
    public String toString()
    {
        return id + ": " + handler.name + ", " + handler.key + ", " + handler.url;
    }

}
