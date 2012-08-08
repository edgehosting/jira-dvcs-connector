package com.atlassian.jira.plugins.dvcs.ondemand;

import org.junit.Assert;
import org.junit.Test;

import com.atlassian.jira.plugins.dvcs.ondemand.AccountsConfig.BitbucketAccountInfo;

public class JsonFileBasedAccountsConfigProviderTest
{

    public JsonFileBasedAccountsConfigProviderTest()
    {
        super();
    }
    
    @Test
    public void testJsonReadWithSuccess() throws Throwable {
        
        AccountsConfigProvider provider = getCustomProvider("ondemand/ondemand.properties");

        AccountsConfig configuration = provider.provideConfiguration();
        
        BitbucketAccountInfo linkConfig = configuration.getSysadminApplicationLinks().get(0).getBitbucket().get(0);

        Assert.assertEquals("mybucketbit", linkConfig.getAccount());
        Assert.assertEquals("verysecretkey", linkConfig.getKey());
        Assert.assertEquals("verysecretsecret", linkConfig.getSecret());
        
    }
    
    @Test
    public void testJsonReadWithSuccessMoreData() throws Throwable {
        
        AccountsConfigProvider provider = getCustomProvider("ondemand/ondemand-more-data.properties");

        AccountsConfig configuration = provider.provideConfiguration();
        
        BitbucketAccountInfo linkConfig = configuration.getSysadminApplicationLinks().get(0).getBitbucket().get(0);

        Assert.assertEquals("mybucketbit", linkConfig.getAccount());
        Assert.assertEquals("verysecretkey", linkConfig.getKey());
        Assert.assertEquals("verysecretsecret", linkConfig.getSecret());
        
    }
    
    @Test
    public void testJsonReadInvalidFile() throws Throwable {

        
        AccountsConfigProvider provider = getCustomProvider("ondemand/ondemand-failed-content.properties");

        AccountsConfig configuration = provider.provideConfiguration();
        
        Assert.assertNull(configuration);
    }
    
    @Test
    public void testJsonReadFileNotFound() throws Throwable {

        
        AccountsConfigProvider provider = getCustomProviderAbsolutePath("/ondemand/ondemand-NOTFOUND.properties");

        AccountsConfig configuration = provider.provideConfiguration();
        
        Assert.assertNull(configuration);
        
    }
    
    
    private AccountsConfigProvider getCustomProvider(String pathWithinTestResources) {

        return getCustomProviderAbsolutePath(getFilePath(pathWithinTestResources));

    }
    
    private AccountsConfigProvider getCustomProviderAbsolutePath(String absolutePath) {

        JsonFileBasedAccountsConfigProvider provider = new JsonFileBasedAccountsConfigProvider();
        provider.setAbsoluteConfigFilePath(absolutePath);
        return provider;

    }
    
    private String getFilePath(String pathWithinTestResources) {

        return getClass().getClassLoader().getResource(pathWithinTestResources).getPath();

    }
    
}

