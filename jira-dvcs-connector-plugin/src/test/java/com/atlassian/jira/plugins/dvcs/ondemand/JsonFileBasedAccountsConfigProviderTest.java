package com.atlassian.jira.plugins.dvcs.ondemand;

import com.atlassian.jira.config.CoreFeatures;
import com.atlassian.jira.config.FeatureManager;
import com.atlassian.jira.plugins.dvcs.ondemand.AccountsConfig.BitbucketAccountInfo;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.when;


public class JsonFileBasedAccountsConfigProviderTest
{
    @Mock
    private FeatureManager featureManager;
    
    @BeforeMethod
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);

        when(featureManager.isEnabled(isA(CoreFeatures.class))).thenReturn(Boolean.TRUE);
    }
    
    @Test
    public void testJsonReadWithSuccess() throws Throwable
    {
        AccountsConfigProvider provider = getCustomProvider("ondemand/ondemand.properties");
        AccountsConfig configuration = provider.provideConfiguration();
        BitbucketAccountInfo linkConfig = configuration.getFirstBitbucketAccountConfig();

        assertThat(linkConfig.getAccount()).isEqualTo("mybucketbit");
        assertThat(linkConfig.getKey())    .isEqualTo("verysecretkey");
        assertThat(linkConfig.getSecret()) .isEqualTo("verysecretsecret");
    }
    
    @Test
    public void testJsonReadWithSuccessMoreData() throws Throwable
    {
        AccountsConfigProvider provider = getCustomProvider("ondemand/ondemand-more-data.properties");
        AccountsConfig configuration = provider.provideConfiguration();
        BitbucketAccountInfo linkConfig = configuration.getFirstBitbucketAccountConfig();

        assertThat(linkConfig.getAccount()).isEqualTo("mybucketbit");
        assertThat(linkConfig.getKey())    .isEqualTo("verysecretkey");
        assertThat(linkConfig.getSecret()) .isEqualTo("verysecretsecret");
    }
    
    @Test
    public void testJsonReadInvalidFile() throws Throwable
    {
        AccountsConfigProvider provider = getCustomProvider("ondemand/ondemand-failed-content.properties");
        AccountsConfig configuration = provider.provideConfiguration();
        assertThat(configuration).isNull();
    }
    
    @Test
    public void testJsonReadFileNotFound() throws Throwable
    {
        AccountsConfigProvider provider = getCustomProvider("/ondemand/ondemand-NOTFOUND.properties");
        AccountsConfig configuration = provider.provideConfiguration();
        assertThat(configuration).isNull();
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

