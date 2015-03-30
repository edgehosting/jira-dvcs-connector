package com.atlassian.jira.plugins.dvcs.dao.impl;

import com.atlassian.cache.CacheManager;
import com.atlassian.cache.CacheSettings;
import com.atlassian.cache.CachedReference;
import com.atlassian.cache.Supplier;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.fugue.Option;
import com.atlassian.jira.bc.dataimport.ImportCompletedEvent;
import com.atlassian.jira.plugins.dvcs.dao.OrganizationAOFacade;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Group;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.List;

import static com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator.BITBUCKET;
import static com.atlassian.jira.plugins.dvcs.spi.github.GithubCommunicator.GITHUB;
import static com.atlassian.jira.plugins.dvcs.spi.githubenterprise.GithubEnterpriseCommunicator.GITHUB_ENTERPRISE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith (MockitoJUnitRunner.class)
public class CachingOrganizationDaoImplTest
{
    private static final String BB_URL = "bitbucket-url";
    private static final String BB_ACCOUNT_NAME = "bitbucket-account";
    private static final String GH_URL = "github-url";
    private static final String GH_ACCOUNT_NAME = "github-account";

    @Mock
    private CacheManager cacheManager;
    @Mock
    private OrganizationAOFacade organizationAOFacade;
    @Mock
    private CachedReference cache;
    @Mock
    private EventPublisher eventPublisher;

    private final Organization orgBitbucket = new Organization(1, BB_URL, BB_ACCOUNT_NAME, BITBUCKET, false,
            new Credential("oauthKey", "oauthSecret", "accessToken"), "organizationUrl", false, new HashSet<Group>());

    private final Organization orgGithub = new Organization(2, GH_URL, GH_ACCOUNT_NAME, GITHUB, false,
            new Credential("oauthKey", "oauthSecret", null), "organizationUrl", false, new HashSet<Group>());

    private final List<Organization> orgs = ImmutableList.of(orgBitbucket, orgGithub);

    private CachingOrganizationDaoImpl cachingOrganizationDao;

    @Before
    public void setUp() throws Exception
    {
        when(cacheManager.getCachedReference(anyString(), any(Supplier.class), any(CacheSettings.class))).thenReturn(cache);
        cachingOrganizationDao = new CachingOrganizationDaoImpl(cacheManager);
        ReflectionTestUtils.setField(cachingOrganizationDao, "organizationAOFacade", organizationAOFacade);
        ReflectionTestUtils.setField(cachingOrganizationDao, "eventPublisher", eventPublisher);

        when(cache.get()).thenReturn(orgs);
        when(organizationAOFacade.save(orgBitbucket)).thenReturn(orgBitbucket);
    }

    @Test
    public void testGetAll() throws Exception
    {
        assertThat(cachingOrganizationDao.getAll(), is(orgs));
    }

    @Test
    public void testGetAllByType() throws Exception
    {
        List<Organization> orgs = cachingOrganizationDao.getAllByType(GITHUB);
        assertThat(orgs.size(), is(1));
        assertThat(orgs.get(0), is(orgGithub));

        orgs = cachingOrganizationDao.getAllByType(BITBUCKET);
        assertThat(orgs.size(), is(1));
        assertThat(orgs.get(0), is(orgBitbucket));

        assertThat(cachingOrganizationDao.getAllByType(GITHUB_ENTERPRISE).size(), is(0));
    }

    @Test
    public void testGetAllCount()
    {
        assertThat(cachingOrganizationDao.getAllCount(), is(2));
    }

    @Test
    public void testGet() throws Exception
    {
        assertThat(cachingOrganizationDao.get(1), is(orgBitbucket));
        assertThat(cachingOrganizationDao.get(2), is(orgGithub));
        assertThat(cachingOrganizationDao.get(3), nullValue());
    }

    @Test
    public void testGetByHostAndName() throws Exception
    {
        assertThat(cachingOrganizationDao.getByHostAndName(BB_URL, BB_ACCOUNT_NAME), is(orgBitbucket));
        assertThat(cachingOrganizationDao.getByHostAndName(GH_URL, GH_ACCOUNT_NAME), is(orgGithub));
        assertThat(cachingOrganizationDao.getByHostAndName("some url", "some account"), nullValue());
    }

    @Test
    public void testGetAllByIds() throws Exception
    {
        List<Organization> orgs = cachingOrganizationDao.getAllByIds(ImmutableList.of(2));
        assertThat(orgs.size(), is(1));
        assertThat(orgs.get(0), is(orgGithub));
    }

    @Test
    public void testExistsOrganizationWithType() throws Exception
    {
        assertThat(cachingOrganizationDao.existsOrganizationWithType(GITHUB), is(true));
        assertThat(cachingOrganizationDao.existsOrganizationWithType(BITBUCKET), is(true));
        assertThat(cachingOrganizationDao.existsOrganizationWithType(BITBUCKET, GITHUB), is(true));
        assertThat(cachingOrganizationDao.existsOrganizationWithType(GITHUB_ENTERPRISE), is(false));
    }

    @Test
    public void testFindIntegratedAccount() throws Exception
    {
        assertThat(cachingOrganizationDao.findIntegratedAccount(), is(orgGithub));

        // when there is no integrated account
        when(cache.get()).thenReturn(ImmutableList.of(orgBitbucket));
        assertThat(cachingOrganizationDao.findIntegratedAccount(), nullValue());
    }

    @Test
    public void testRemove() throws Exception
    {
        final int orgId = 1;
        cachingOrganizationDao.remove(orgId);

        verify(organizationAOFacade).remove(orgId);
        verify(cache).reset();
    }

    @Test
    public void testSave() throws Exception
    {
        cachingOrganizationDao.save(orgBitbucket);

        verify(organizationAOFacade).save(orgBitbucket);
        verify(cache).reset();
    }

    @Test
    public void testSetDefaultGroupsSlugs() throws Exception
    {
        final List<String> slugs = ImmutableList.of("slug1");
        final int orgId = 2;

        cachingOrganizationDao.setDefaultGroupsSlugs(orgId, slugs);

        verify(organizationAOFacade).updateDefaultGroupsSlugs(orgId, slugs);
        verify(cache).reset();
    }

    @Test
    public void testOnImportCompleted()
    {
        cachingOrganizationDao.onImportCompleted(new ImportCompletedEvent(true, Option.none(Long.class)));
        verify(cache).reset();
    }

    @Test
    public void testRegisterListener()
    {
        cachingOrganizationDao.registerListener();
        verify(eventPublisher).register(cachingOrganizationDao);
    }

    @Test
    public void testUnregisterListener()
    {
        cachingOrganizationDao.unregisterListener();
        verify(eventPublisher).unregister(cachingOrganizationDao);
    }
}