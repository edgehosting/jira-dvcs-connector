package com.atlassian.jira.plugins.dvcs.ondemand;

import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.ondemand.AccountsConfig.BitbucketAccountInfo;
import com.atlassian.jira.plugins.dvcs.ondemand.AccountsConfig.Links;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.PluginController;
import com.atlassian.plugin.web.descriptors.WebFragmentModuleDescriptor;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class BitbucketAccountsConfigServiceTest
{
    @Mock
    private OrganizationService organizationService;

    @Mock
    private AccountsConfigProvider configProvider;

    @Mock
    private PluginController pluginController;

    @Mock
    private PluginAccessor pluginAccessor;

    @Mock
    private WebFragmentModuleDescriptor webFragmentModuleDescriptor;

    @Mock
    private BitbucketAccountsReloadJobScheduler mockBitbucketAccountsReloadJobScheduler;

    @Captor
    ArgumentCaptor<Organization> organizationCaptor;

    private BitbucketAccountsConfigService testedService;

    public BitbucketAccountsConfigServiceTest()
    {
        super();
    }

    @BeforeMethod
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);

        testedService = new BitbucketAccountsConfigService(configProvider, organizationService,
                mockBitbucketAccountsReloadJobScheduler, pluginController, pluginAccessor);

        when(configProvider.supportsIntegratedAccounts()).thenReturn(true);
        when(pluginAccessor.getEnabledPluginModule(anyString())).thenReturn(webFragmentModuleDescriptor);
    }

    @Test
    public void testAddNewAccountWithSuccess()
    {

        AccountsConfig correctConfig = createCorrectConfig();
        when(configProvider.provideConfiguration()).thenReturn(correctConfig);
        when(organizationService.findIntegratedAccount()).thenReturn(null);
        when(organizationService.getByHostAndName(eq("https://bitbucket.org"), eq("A"))).thenReturn(null);

        testedService.reload();

        verify(organizationService).save(organizationCaptor.capture());

        Organization savedOrg = organizationCaptor.getValue();

        assertThat(savedOrg.getName()).isEqualTo("A");
        assertThat(savedOrg.getCredential().getOauthKey()).isEqualTo("K");
        assertThat(savedOrg.getCredential().getOauthSecret()).isEqualTo("S");
    }

    @Test
    public void testAddNewAccountEmptyConfig()
    {

        when(configProvider.provideConfiguration()).thenReturn(null);
        when(organizationService.findIntegratedAccount()).thenReturn(null);

        testedService.reload();

        verify(organizationService, times(0)).save(organizationCaptor.capture());

    }

    @Test
    public void testUpdateAccountEmptyConfig()
    {

        when(configProvider.provideConfiguration()).thenReturn(null);

        Organization existingAccount = createSampleAccount("A", "B", "S", "token");
        when(organizationService.findIntegratedAccount()).thenReturn(existingAccount);

        testedService.reload();

        verify(organizationService, times(0)).save(organizationCaptor.capture());

        verify(organizationService).remove(eq(5));
    }

    @Test
    public void testUpdateAccountCredentialsWithSuccess()
    {
        AccountsConfig correctConfig = createCorrectConfig();
        when(configProvider.provideConfiguration()).thenReturn(correctConfig);

        Organization existingAccount = createSampleAccount("A", "B", "S", "token");
        when(organizationService.findIntegratedAccount()).thenReturn(existingAccount);
        when(organizationService.getByHostAndName(eq("https://bitbucket.org"), eq("A"))).thenReturn(null);

        testedService.reload();

        verify(organizationService).findIntegratedAccount();
        verify(organizationService).getByHostAndName("https://bitbucket.org", "A");
        verifyNoMoreInteractions(organizationService);
    }

    @Test
    public void testUpdateAccountCredentialsEmptyConfig_ShouldRemoveIntegratedAccount()
    {

        when(configProvider.provideConfiguration()).thenReturn(null);

        Organization existingAccount = createSampleAccount("A", "B", "S", "token");
        when(organizationService.findIntegratedAccount()).thenReturn(existingAccount);

        when(organizationService.getByHostAndName(eq("https://bitbucket.org"), eq("A"))).thenReturn(null);

        testedService.reload();

        verify(organizationService).remove(eq(5));

    }

    //--

    @Test
    public void testAddNewAccountWithSuccess_UserAddedAccountExists()
    {

        AccountsConfig config = createCorrectConfig();
        when(configProvider.provideConfiguration()).thenReturn(config);

        when(organizationService.findIntegratedAccount()).thenReturn(null);

        Organization userAddedAccount = createSampleAccount("A", "key", "secret", "token");
        when(organizationService.getByHostAndName(eq("https://bitbucket.org"), eq("A"))).thenReturn(userAddedAccount);

        testedService.reload();

        verify(organizationService).updateCredentials(userAddedAccount.getId(), new Credential("K", "S", null));
    }

    //--


    private Organization createSampleAccount(String name, String key, String secret, String token)
    {
        Organization org = new Organization();
        org.setId(5);
        org.setName(name);
        org.setCredential(new Credential(key, secret, token));
        return org;
    }

    private AccountsConfig createCorrectConfig()
    {
        return createConfig("A", "K", "S");
    }

    private AccountsConfig createConfig(String account, String key, String secret)
    {
        AccountsConfig accountsConfig = new AccountsConfig();
        List<Links> sysadminApplicationLinks = new ArrayList<AccountsConfig.Links>();
        Links links = new Links();
        List<BitbucketAccountInfo> bbLinks = new ArrayList<AccountsConfig.BitbucketAccountInfo>();
        BitbucketAccountInfo bbAccount = new BitbucketAccountInfo();
        bbAccount.setAccount(account);
        bbAccount.setKey(key);
        bbAccount.setSecret(secret);
        bbLinks.add(bbAccount);
        links.setBitbucket(bbLinks);
        sysadminApplicationLinks.add(links);
        accountsConfig.setSysadminApplicationLinks(sysadminApplicationLinks);
        return accountsConfig;
    }

}
