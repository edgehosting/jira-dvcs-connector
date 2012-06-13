package it.com.atlassian.jira.plugins.dvcs;

import static junit.framework.Assert.*;

import javax.ws.rs.core.MediaType;

import org.apache.commons.httpclient.HttpStatus;
import org.junit.Test;

import com.atlassian.jira.plugins.dvcs.RestUrlBuilder;
import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;

public class TestAccountInfoDetection
{

    @Test
    public void testAccountInfoBitbucket()
    {
        String url = new RestUrlBuilder("/rest/bitbucket/1.0/accountInfo")
            .add("server", "https://bitbucket.org")
            .add("account", "dusanhornik")
            .toString();
        
        AccountInfo accountInfo = new Client()
            .resource(url)
            .accept(MediaType.APPLICATION_XML_TYPE)
            .get(AccountInfo.class);
        
        assertEquals("bitbucket", accountInfo.getDvcsType());
    }

    @Test
    public void testAccountInfoBitbucketInvalid()
    {
        String url = new RestUrlBuilder("/rest/bitbucket/1.0/accountInfo")
            .add("server", "https://bitbucket.org")
            .add("account", "invalidAccount")
            .toString();
        
        
        try
        {
            new Client()
                .resource(url)
                .accept(MediaType.APPLICATION_XML_TYPE)
                .get(AccountInfo.class);
            fail("Expected 404 Not found");
        } catch (UniformInterfaceException e)
        {
            assertEquals("Expecting 404 Not found",HttpStatus.SC_NOT_FOUND, e.getResponse().getStatus());
        }

    }

    @Test
    public void testAccountInfoGithub()
    {
        String url = new RestUrlBuilder("/rest/bitbucket/1.0/accountInfo")
            .add("server", "https://github.com")
            .add("account", "dusanhornik")
            .toString();
        
        AccountInfo accountInfo = new Client()
            .resource(url)
            .accept(MediaType.APPLICATION_XML_TYPE)
            .get(AccountInfo.class);

        assertEquals("github", accountInfo.getDvcsType());
    }

    @Test
    public void testAccountInfoGithubInvalid()
    {
        String url = new RestUrlBuilder("/rest/bitbucket/1.0/accountInfo")
            .add("server", "https://github.com")
            .add("account", "invalidAccount")
            .toString();
        
        try
        {
            new Client()
                .resource(url)
                .accept(MediaType.APPLICATION_XML_TYPE)
                .get(AccountInfo.class);
            fail("Expected 404 Not found");
        } catch (UniformInterfaceException e)
        {
            assertEquals("Expecting 404 Not found",HttpStatus.SC_NOT_FOUND, e.getResponse().getStatus());
        }
    }

}
