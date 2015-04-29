package it.com.atlassian.jira.plugins.dvcs.cleanup;

import com.atlassian.jira.plugins.dvcs.pageobjects.common.MagicVisitor;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitbucketLoginPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitbucketOAuthPage;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketConsumer;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.AuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BasicAuthAuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.HttpClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.ConsumerRemoteRestpoint;
import com.beust.jcommander.internal.Lists;

import java.util.List;

public class DeleteBitbucketOrphanAppsTest extends DeleteOrphanAppsBaseTest
{
    @Override
    protected void deleteOrphanOAuthApplications(final String repoOwner, final String repoPassword)
    {
        goToOAuthPage(repoOwner);
        List<BitbucketConsumer> expiredConsumers = findExpiredConsumers(repoOwner, repoPassword);

        for (BitbucketConsumer consumer : expiredConsumers)
        {
            removeConsumer(repoOwner, consumer.getId().toString());
        }
    }

    @Override
    protected void login(final String repoOwner, final String repoPassword)
    {
        new MagicVisitor(jira).visit(BitbucketLoginPage.class).doLogin(repoOwner, repoPassword);
    }

    @Override
    protected void logout()
    {
        new MagicVisitor(jira).visit(BitbucketLoginPage.class).doLogout();
    }

    private ConsumerRemoteRestpoint createConsumerRemoteRestpoint(final String repoOwner, final String repoPassword)
    {
        HttpClientProvider httpClientProvider = new HttpClientProvider();
        httpClientProvider.setUserAgent(BitbucketRemoteClient.TEST_USER_AGENT);

        AuthProvider basicAuthProvider = new BasicAuthAuthProvider(BitbucketRemoteClient.BITBUCKET_URL,
                repoOwner, repoPassword, httpClientProvider);

        return new ConsumerRemoteRestpoint(basicAuthProvider.provideRequestor());
    }

    private BitbucketOAuthPage goToOAuthPage(final String repoOwner)
    {
        return new MagicVisitor(jira).visit(BitbucketOAuthPage.class, repoOwner);
    }

    /**
     * @return list of expired consumers to be deleted from Bitbucket
     */
    private List<BitbucketConsumer> findExpiredConsumers(final String repoOwner, final String repoPassword)
    {
        ConsumerRemoteRestpoint consumerRemoteRestpoint = createConsumerRemoteRestpoint(repoOwner, repoPassword);
        List<BitbucketConsumer> expiredConsumers = Lists.newArrayList();

        List<BitbucketConsumer> consumers = consumerRemoteRestpoint.getConsumers(repoOwner);
        for (BitbucketConsumer consumer : consumers)
        {
            if (isConsumerExpired(consumer.getName()))
            {
                expiredConsumers.add(consumer);
            }
        }
        return expiredConsumers;
    }

    private void removeConsumer(final String repoOwner, final String applicationId)
    {
        new MagicVisitor(jira).visit(BitbucketOAuthPage.class, repoOwner).removeConsumer(applicationId);
    }
}
