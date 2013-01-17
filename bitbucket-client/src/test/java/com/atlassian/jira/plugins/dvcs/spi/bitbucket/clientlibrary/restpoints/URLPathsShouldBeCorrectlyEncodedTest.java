package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;

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
    
    @Test(expectedExceptions = BitbucketRequestException.BadRequest_400.class)
    public void performingPOSTRequestWithOAuthAuthentication_ShouldNotThrowProtocolException()
    {
        // performing POST with not properly encoded URL used to return HTTP 302 status code
        accountRemoteRestpoint.inviteUser("a", "b", "c", "d e");
    }

    @Test()
    public void performingPOSTRequestWithOAuthAuthentication() throws UnsupportedEncodingException
    {
        BitbucketRemoteClient bitbucketRemoteClientWithOAuthAuth =
                new BitbucketRemoteClient(new ThreeLegged10aOauthProvider(BitbucketRemoteClient.BITBUCKET_URL,
                                                                          "pJyXFLwRqM5DTGT8eZ",
                                                                          "mRPP9VmWqqZ38uhHMEZbPuP4JSkPkfb9",
                                                                          "evECs8jFrMMLWwLdq8&pYqB8EFRqVWksMBWjTrhmT5YCjs47gxU"));

        AccountRemoteRestpoint rest = bitbucketRemoteClientWithOAuthAuth.getAccountRest();
        rest.modifyService("369581", "hotovo", "binu-facebook-application", "http:/some.url/com" + new Date());

        // performing POST with not properly encoded URL used to return HTTP 302 status code
        String email = URLEncoder.encode("klinec+test5@gmail.com", "UTF-8");
        rest.inviteUser("hotovo", email, "binu", "hotovo");
    }
}
