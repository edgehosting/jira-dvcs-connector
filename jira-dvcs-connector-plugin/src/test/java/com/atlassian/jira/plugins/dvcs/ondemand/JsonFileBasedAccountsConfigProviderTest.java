package com.atlassian.jira.plugins.dvcs.ondemand;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import static org.mockito.Mockito.*;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.atlassian.jira.config.CoreFeatures;
import com.atlassian.jira.config.FeatureManager;
import com.atlassian.jira.plugins.dvcs.ondemand.AccountsConfig.BitbucketAccountInfo;


@RunWith(MockitoJUnitRunner.class)
public class JsonFileBasedAccountsConfigProviderTest
{
    @Mock
    private FeatureManager featureManager;
    
    @Before
    public void setUp()
    {
        when(featureManager.isEnabled(isA(CoreFeatures.class))).thenReturn(Boolean.TRUE);
    }
    
    @Test
    public void testJsonReadWithSuccess() throws Throwable
    {
        AccountsConfigProvider provider = getCustomProvider("ondemand/ondemand.properties");
        AccountsConfig configuration = provider.provideConfiguration();
        BitbucketAccountInfo linkConfig = configuration.getFirstBitbucketAccountConfig();

        Assert.assertEquals("mybucketbit", linkConfig.getAccount());
        Assert.assertEquals("verysecretkey", linkConfig.getKey());
        Assert.assertEquals("verysecretsecret", linkConfig.getSecret());
    }
    
    @Test
    public void testJsonReadWithSuccessMoreData() throws Throwable
    {
        AccountsConfigProvider provider = getCustomProvider("ondemand/ondemand-more-data.properties");
        AccountsConfig configuration = provider.provideConfiguration();
        BitbucketAccountInfo linkConfig = configuration.getFirstBitbucketAccountConfig();

        Assert.assertEquals("mybucketbit", linkConfig.getAccount());
        Assert.assertEquals("verysecretkey", linkConfig.getKey());
        Assert.assertEquals("verysecretsecret", linkConfig.getSecret());
    }
    
    @Test
    public void testJsonReadInvalidFile() throws Throwable
    {
        AccountsConfigProvider provider = getCustomProvider("ondemand/ondemand-failed-content.properties");
        AccountsConfig configuration = provider.provideConfiguration();
        Assert.assertNull(configuration);
    }
    
    @Test
    public void testJsonReadFileNotFound() throws Throwable
    {
        AccountsConfigProvider provider = getCustomProvider("/ondemand/ondemand-NOTFOUND.properties");
        AccountsConfig configuration = provider.provideConfiguration();
        Assert.assertNull(configuration);
    }
    
    
    private AccountsConfigProvider getCustomProvider(String pathWithinTestResources)
    {
        return getCustomProviderAbsolutePath(getFileURL(pathWithinTestResources));
    }
    
    private AccountsConfigProvider getCustomProviderAbsolutePath(final URL configFileUrl)
    {
        JsonFileBasedAccountsConfigProvider provider = new JsonFileBasedAccountsConfigProvider(featureManager)
        {
            @Override
            File getConfigFile()
            {
                try
                {
                    return new File(configFileUrl.toURI());
                } catch (URISyntaxException e)
                {
                    throw new RuntimeException(e);
                }
            }
        };
        return provider;

    }
    
    private URL getFileURL(String pathWithinTestResources)
    {
        return getClass().getClassLoader().getResource(pathWithinTestResources);
    }
}

