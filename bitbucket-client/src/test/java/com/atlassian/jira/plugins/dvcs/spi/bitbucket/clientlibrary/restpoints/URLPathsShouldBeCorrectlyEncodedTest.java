package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BitbucketRequestException;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.HttpClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.scribe.ThreeLegged10aOauthProvider;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * This test is about getting rid of unnecessary "java.net.ProtocolException: Server redirected too many  times (20)".
 * That was caused by not properly encoded URL paths together with OAuth authentication.
 *
 * @author Martin Skurla
 */
public class URLPathsShouldBeCorrectlyEncodedTest
{
    private static final String OAUTH_KEY          = "fZwXWrQcrcKPeAJffL";
    private static final String OAUTH_SECRET       = "QpEPRhpjW6j8q8QkwCDdByDtDAd6FEfG";
    private static final String OAUTH_ACCESS_TOKEN = "R59tgJZAjPkRMuJasW&YMtyNdna9pjCnTBNjnygsnU9Vexr7jDf";

    private static AccountRemoteRestpoint accountRemoteRestpoint;


    @BeforeClass
    public static void initializeAccountREST()
    {
        HttpClientProvider httpClientProvider = new HttpClientProvider();

        httpClientProvider.setUserAgent("jirabitbucketconnector-test");

        BitbucketRemoteClient bitbucketRemoteClientWithOAuthAuth =
                new BitbucketRemoteClient(new ThreeLegged10aOauthProvider(BitbucketRemoteClient.BITBUCKET_URL,
                                                                          OAUTH_KEY,
                                                                          OAUTH_SECRET,
                                                                          OAUTH_ACCESS_TOKEN,
                                                                          httpClientProvider));

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
     * If you use encoded email address Bitbucket returns 401 - Unauthorized (wtf)
     */
    @Test(expectedExceptions = BitbucketRequestException.Unauthorized_401.class)
    public void performingPUTRequestWithOAuthAuthentication_ShouldThrow401Exception()
    {
        accountRemoteRestpoint.inviteUser("hotovo", "some_email%40gmail.com", "hotovo", "binu");
    }
}
