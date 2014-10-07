package it.com.atlassian.jira.plugins.dvcs;

import com.atlassian.jira.plugins.dvcs.pageobjects.RestUrlBuilder;
import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import org.apache.commons.httpclient.HttpStatus;
import org.testng.annotations.Test;

import javax.ws.rs.core.MediaType;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;

public class TestAccountInfoDetection extends DvcsWebDriverTestCase
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
        
        assertThat(accountInfo.getDvcsType()).isEqualTo("bitbucket");
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
            assertThat(e.getResponse().getStatus()).isEqualTo(HttpStatus.SC_NOT_FOUND);
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

        assertThat(accountInfo.getDvcsType()).isEqualTo("github");
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
            assertThat(e.getResponse().getStatus()).isEqualTo(HttpStatus.SC_NOT_FOUND);
        }
    }

}
