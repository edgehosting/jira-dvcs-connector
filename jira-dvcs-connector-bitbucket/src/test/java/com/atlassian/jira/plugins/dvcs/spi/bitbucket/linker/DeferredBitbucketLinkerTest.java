package com.atlassian.jira.plugins.dvcs.spi.bitbucket.linker;

import com.atlassian.beehive.ClusterLock;
import com.atlassian.beehive.ClusterLockService;
import com.atlassian.beehive.compat.ClusterLockServiceFactory;
import com.atlassian.jira.plugins.dvcs.DumbClusterLockServiceFactory;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import org.fest.util.Sets;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Set;

import static com.atlassian.jira.plugins.dvcs.util.DvcsConstants.LINKERS_ENABLED_SETTINGS_PARAM;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class DeferredBitbucketLinkerTest
{
    private static final Set<String> PROJECT_KEYS = Sets.newLinkedHashSet("foo", "bar");
    private static final String REPO_URL = "ssh://git@bitbucket.org/atlassian/foobar.git";

    @Mock
    private BitbucketLinker mockBitbucketLinker;

    @Mock
    private ClusterLockService mockClusterLockService;

    @Mock
    private ClusterLock mockLock;

    @Mock
    private PluginSettings mockPluginSettings;

    @Mock
    private PluginSettingsFactory mockPluginSettingsFactory;

    @Mock
    private Repository mockRepository;

    private DeferredBitbucketLinker linkerUnderTest;

    private String lockName;

    @BeforeMethod
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        when(mockPluginSettingsFactory.createGlobalSettings()).thenReturn(mockPluginSettings);
        when(mockRepository.getRepositoryUrl()).thenReturn(REPO_URL);
        lockName = DeferredBitbucketLinker.getLockName(mockRepository);
        final ClusterLockServiceFactory clusterLockServiceFactory =
                new DumbClusterLockServiceFactory(mockClusterLockService);
        linkerUnderTest = new DeferredBitbucketLinker(
                mockBitbucketLinker, clusterLockServiceFactory, mockPluginSettingsFactory);
    }

    @Test
    public void shouldLinkRepositoryWhenLinkersEnabledHasDefaultValue()
    {
        assertRepositoryIsLinked(null);
    }

    @Test
    public void shouldLinkRepositoryWhenLinkersAreExplicitlyEnabled()
    {
        assertRepositoryIsLinked(Boolean.toString(true));
    }

    private void assertRepositoryIsLinked(final String linkersEnabled)
    {
        // Set up
        setUpLinkersEnabled(linkersEnabled);
        when(mockClusterLockService.getLockForName(lockName)).thenReturn(mockLock);

        // Invoke
        linkerUnderTest.linkRepository(mockRepository, PROJECT_KEYS);

        // Check
        verify(mockLock).lock();
        verify(mockBitbucketLinker).linkRepository(mockRepository, PROJECT_KEYS);
        verify(mockLock).unlock();
        verifyNoMoreInteractions(mockBitbucketLinker, mockLock);
    }

    @Test
    public void shouldNotLinkRepositoryWhenLinkersAreDisabled()
    {
        // Set up
        setUpLinkersEnabled(Boolean.toString(false));

        // Invoke
        linkerUnderTest.linkRepository(mockRepository, PROJECT_KEYS);

        // Check
        verifyNoMoreInteractions(mockBitbucketLinker, mockClusterLockService, mockLock);
    }

    @Test
    public void shouldUnlinkRepositoryWhenLinkersEnabledHasDefaultValue()
    {
        // Set up
        setUpLinkersEnabled(null);
        when(mockClusterLockService.getLockForName(lockName)).thenReturn(mockLock);

        // Invoke
        linkerUnderTest.unlinkRepository(mockRepository);

        // Check
        verify(mockLock).lock();
        verify(mockBitbucketLinker).unlinkRepository(mockRepository);
        verify(mockLock).unlock();
        verifyNoMoreInteractions(mockBitbucketLinker, mockLock);
    }

    private void setUpLinkersEnabled(final String linkersEnabled)
    {
        when(mockPluginSettings.get(LINKERS_ENABLED_SETTINGS_PARAM)).thenReturn(linkersEnabled);
    }
}
