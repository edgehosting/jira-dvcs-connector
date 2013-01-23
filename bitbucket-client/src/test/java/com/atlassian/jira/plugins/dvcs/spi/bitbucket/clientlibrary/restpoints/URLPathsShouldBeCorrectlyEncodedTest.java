package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BitbucketRequestException;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.scribe.ThreeLegged10aOauthProvider;

/**
 * This test is about getting rid of unnecessary "java.net.ProtocolException: Server redirected too many  times (20)".
 * That was caused by not properly encoded URL paths together with OAuth authentication.
 *
 * @author Martin Skurla
 */
public class URLPathsShouldBeCorrectlyEncodedTest
{
    private static final String OAUTH_KEY          = "PEruYUbd3vTdnSPYez";
    private static final String OAUTH_SECRET       = "hwP4CP9ZtAPMR9UJH59q8mnAWx7NSRpp";
    private static final String OAUTH_ACCESS_TOKEN = "qUeN7w3cGVwFhgcDtu&T7Ex3kMQ6TzAg8GeskdeD3ERLPQc65sV";

    private static AccountRemoteRestpoint accountRemoteRestpoint;


    @BeforeClass
    public static void initializeAccountREST()
    {
        BitbucketRemoteClient bitbucketRemoteClientWithOAuthAuth =
                new BitbucketRemoteClient(new ThreeLegged10aOauthProvider(BitbucketRemoteClient.BITBUCKET_URL,
                                                                          OAUTH_KEY,
                                                                          OAUTH_SECRET,
                                                                          OAUTH_ACCESS_TOKEN));

        accountRemoteRestpoint = bitbucketRemoteClientWithOAuthAuth.getAccountRest();
    }


    @Test(expectedExceptions = BitbucketRequestException.NotFound_404.class)
    public void performingGETRequestWithOAuthAuthentication_ShouldNotThrowProtocolException()
    {
        // performing GET with not properly encoded URL used to throw ProtocolException
        accountRemoteRestpoint.getUser("fake User");
    }
    
    @Test(expectedExceptions = BitbucketRequestException.Forbidden_403.class)
    public void performingPUTRequestWithOAuthAuthentication_ShouldNotThrowProtocolException()
    {
        accountRemoteRestpoint.inviteUser("hotovo", "some_email@gmail.com", "hotovo", "binu");
    }
    
    
    /**
     * If you use encoded email address Bitbucket returns 302 - redirection to log in page (wtf) 
     */
    @Test(expectedExceptions = BitbucketRequestException.class, expectedExceptionsMessageRegExp = "Error response code during the request : 302")
    public void performingPUTRequestWithOAuthAuthentication_ShouldThrow302Exception()
    {
        accountRemoteRestpoint.inviteUser("hotovo", "some_email%40gmail.com", "hotovo", "binu");
    }
}