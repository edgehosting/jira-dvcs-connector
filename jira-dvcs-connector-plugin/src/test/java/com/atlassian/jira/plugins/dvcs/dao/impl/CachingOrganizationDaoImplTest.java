package com.atlassian.jira.plugins.dvcs.dao.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheLoader;
import com.atlassian.cache.CacheManager;
import com.atlassian.cache.CacheSettings;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.OrganizationMapping;
import com.atlassian.jira.plugins.dvcs.crypto.Encryptor;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Group;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.InvalidOrganizationManager;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

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
import static org.mockito.Mockito.spy;
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
    private ActiveObjects activeObjects;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private Encryptor encryptor;
    @Mock
    private InvalidOrganizationManager invalidOrganizationsManager;
    @Mock
    private Cache cache;
    @Mock
    private OrganizationMapping organizationMapping;

    private Organization orgBitbucket = new Organization(1, BB_URL, BB_ACCOUNT_NAME, BITBUCKET, false,
            new Credential("oauthKey", "oauthSecret", "accessToken"), "organizationUrl", false, new HashSet<Group>());

    private Organization orgGithub = new Organization(2, GH_URL, GH_ACCOUNT_NAME, GITHUB, false,
            new Credential("oauthKey", "oauthSecret", null), "organizationUrl", false, new HashSet<Group>());

    private List<Organization> orgs = ImmutableList.of(orgBitbucket, orgGithub);

    private CachingOrganizationDaoImpl cachingOrganizationDao;

    @Before
    public void setUp() throws Exception
    {
        when(cacheManager.getCache(anyString(), any(CacheLoader.class), any(CacheSettings.class))).thenReturn(cache);
        cachingOrganizationDao = spy(new CachingOrganizationDaoImpl(activeObjects, cacheManager, encryptor, invalidOrganizationsManager));

        when(cache.get(CachingOrganizationDaoImpl.REPO_CACHE_KEY)).thenReturn(orgs);
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
        when(cache.get(CachingOrganizationDaoImpl.REPO_CACHE_KEY)).thenReturn(ImmutableList.of(orgBitbucket));
        assertThat(cachingOrganizationDao.findIntegratedAccount(), nullValue());
    }

    @Test
    public void testRemove() throws Exception
    {
        when(activeObjects.get(OrganizationMapping.class, 1)).thenReturn(organizationMapping);

        cachingOrganizationDao.remove(1);

        verify(activeObjects).delete(organizationMapping);
        verify(cache).removeAll();
    }

    @Test
    public void testSave() throws Exception
    {
        cachingOrganizationDao.save(orgBitbucket);

        verify(activeObjects).executeInTransaction(any(TransactionCallback.class));
        verify(cache).removeAll();
    }

    @Test
    public void testSetDefaultGroupsSlugs() throws Exception
    {
        when(activeObjects.get(OrganizationMapping.class, 2)).thenReturn(organizationMapping);

        cachingOrganizationDao.setDefaultGroupsSlugs(2, ImmutableList.of("slug1"));

        verify(activeObjects).executeInTransaction(any(TransactionCallback.class));
        verify(cache).removeAll();
    }
}