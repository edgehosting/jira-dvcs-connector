package com.atlassian.jira.plugins.dvcs;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketClientBuilder;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketClientBuilderFactory;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketBranchesAndTags;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketService;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketServiceEnvelope;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketServiceField;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.BranchesAndTagsRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.ChangesetRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.ServiceRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.linker.BitbucketLinker;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.PluginInformation;
import com.atlassian.sal.api.ApplicationProperties;
import com.google.common.collect.Lists;

public class BitbucketCommunicatorTest
{
    @Mock
    private Repository repositoryMock;
    @Mock
    private BitbucketLinker bitbucketLinkerMock;
    @Mock
    private PluginAccessor pluginAccessorMock;
    @Mock
    private BitbucketClientBuilderFactory bitbucketClientBuilderFactoryMock;
    @Mock
    private MessagingService messagingServiceMock;
    @Mock
    private BitbucketRemoteClient bitbucketRemoteClientMock;
    @Mock
    private BranchesAndTagsRemoteRestpoint branchesAndTagsRemoteRestMock;
    @Mock
    private BitbucketBranchesAndTags bitbucketBranchesAndTagsMock;
    @Mock
    private ChangesetRemoteRestpoint changesetRestpoint;
    @Mock
    private Plugin pluginMock;
    @Mock
    private PluginInformation pluginInformationMock;
    @Mock
    private ServiceRemoteRestpoint servicesRestMock;
    @Mock
    private ApplicationProperties applicationPropertiesMock;
    @Mock
    private DvcsCommunicatorProvider dvcsCommunicatorProvider;

    private BitbucketClientBuilder bitbucketClientBuilderMock;
    private DvcsCommunicator communicator;

    @Test
    public void testSetupPostHookShouldDeleteOrphan()
    {
        when(repositoryMock.getOrgName()).thenReturn("owner");
        when(repositoryMock.getSlug()).thenReturn("slug");
        when(servicesRestMock.getAllServices("owner", "slug")).thenReturn(sampleServices());
        when(bitbucketRemoteClientMock.getServicesRest()).thenReturn(servicesRestMock);

        when(applicationPropertiesMock.getBaseUrl()).thenReturn("http://jira.example.com");

        String hookUrl = "http://jira.example.com" + DvcsCommunicator.POST_HOOK_SUFFIX + "5/sync";
        communicator.ensureHookPresent(repositoryMock, hookUrl);

        verify(servicesRestMock, times(1)).addPOSTService("owner", "slug", hookUrl);
        verify(servicesRestMock, times(1)).deleteService("owner", "slug", 111);
        verify(servicesRestMock, times(1)).deleteService("owner", "slug", 101);
    }

    @Test
    public void testSetupPostHookAlreadySetUpShouldDeleteOrphan()
    {
        when(repositoryMock.getOrgName()).thenReturn("owner");
        when(repositoryMock.getSlug()).thenReturn("slug");
        List<BitbucketServiceEnvelope> sampleServices = sampleServices();
        sampleServices.add(sampleService("http://jira.example.com/rest/bitbucket/1.0/repository/5/sync", 1));
        when(servicesRestMock.getAllServices("owner", "slug")).thenReturn(sampleServices);
        when(bitbucketRemoteClientMock.getServicesRest()).thenReturn(servicesRestMock);

        when(applicationPropertiesMock.getBaseUrl()).thenReturn("http://jira.example.com");

        String hookUrl = "http://jira.example.com" + DvcsCommunicator.POST_HOOK_SUFFIX + "5/sync";
        communicator.ensureHookPresent(repositoryMock, hookUrl);

        verify(servicesRestMock, never()).addPOSTService("owner", "slug", hookUrl);
        verify(servicesRestMock, times(1)).deleteService("owner", "slug", 111);
        verify(servicesRestMock, times(1)).deleteService("owner", "slug", 101);
    }

    private List<BitbucketServiceEnvelope> sampleServices()
    {
        List<BitbucketServiceEnvelope> services = Lists.newArrayList();
        services.add(sampleService("http://jira.example.com/rest/bitbucket/1.0/repository/55/sync", 111));
        services.add(sampleService("http://jira.example.com/rest/bitbucket/1.0/repository/54/sync", 101));
        return services;
    }

    private BitbucketServiceEnvelope sampleService(String url, int id)
    {
        BitbucketServiceEnvelope e1 = new BitbucketServiceEnvelope();
        BitbucketService s1 = new BitbucketService();
        s1.setType(ServiceRemoteRestpoint.SERVICE_TYPE_POST);
        List<BitbucketServiceField> fields1 = Lists.newArrayList();
        BitbucketServiceField field11 = new BitbucketServiceField();
        field11.setName("URL");
        field11.setValue(url);
        fields1.add(field11);
        s1.setFields(fields1);
        e1.setService(s1);
        e1.setId(id);
        return e1;
    }

    @BeforeMethod
    public void initializeMocksAndBitbucketCommunicator()
    {
        MockitoAnnotations.initMocks(this);
        
        when(pluginInformationMock.getVersion()).thenReturn("0");
        when(pluginMock.getPluginInformation()).thenReturn(pluginInformationMock);
        when(pluginAccessorMock.getPlugin(anyString())).thenReturn(pluginMock);
        
        bitbucketClientBuilderMock = mock(BitbucketClientBuilder.class, new BuilderAnswer());
        when(bitbucketClientBuilderFactoryMock.forRepository(Matchers.any(Repository.class))).thenReturn(bitbucketClientBuilderMock);
        communicator = new BitbucketCommunicator(bitbucketLinkerMock, pluginAccessorMock, bitbucketClientBuilderFactoryMock, applicationPropertiesMock);
        
        when(bitbucketClientBuilderMock.build()).thenReturn(bitbucketRemoteClientMock);
        when(bitbucketRemoteClientMock.getChangesetsRest()).thenReturn(changesetRestpoint);
        when(bitbucketRemoteClientMock.getBranchesAndTagsRemoteRestpoint()).thenReturn(branchesAndTagsRemoteRestMock);
    }
    
    private static class BuilderAnswer implements Answer<Object>
    {
        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable
        {
            Object builderMock = invocation.getMock();
            if (invocation.getMethod().getReturnType().isInstance(builderMock))
            {
                return builderMock;
            } else
            {
                return Mockito.RETURNS_DEFAULTS.answer(invocation);
            }
        }
    }
}
