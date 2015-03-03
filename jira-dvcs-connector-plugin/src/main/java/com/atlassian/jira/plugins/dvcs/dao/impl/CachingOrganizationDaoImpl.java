package com.atlassian.jira.plugins.dvcs.dao.impl;

import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheLoader;
import com.atlassian.cache.CacheManager;
import com.atlassian.cache.CacheSettings;
import com.atlassian.cache.CacheSettingsBuilder;
import com.atlassian.jira.plugins.dvcs.dao.OrganizationDao;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.apache.commons.lang.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.atlassian.gzipfilter.org.apache.commons.lang.StringUtils.isBlank;
import static com.atlassian.gzipfilter.org.apache.commons.lang.StringUtils.isNotBlank;
import static java.util.concurrent.TimeUnit.MINUTES;

@Component("cachingOrganizationDao")
public class CachingOrganizationDaoImpl implements OrganizationDao
{
    private static final CacheSettings CACHE_SETTINGS = new CacheSettingsBuilder().expireAfterWrite(30, MINUTES).build();
    private final Cache<String, List<Organization>> organizationsCache;

    @Autowired
    @Qualifier("organizationDao")
    private OrganizationDao organizationDao;

    @VisibleForTesting
    static final String REPO_CACHE_KEY = "all";

    @Autowired
    public CachingOrganizationDaoImpl(@ComponentImport final CacheManager cacheManager)
    {
        organizationsCache = cacheManager.getCache(getClass().getName() + ".organizationsCache", new OrganizationLoader(), CACHE_SETTINGS);
    }

    @Override
    public List<Organization> getAll()
    {
        return organizationsCache.get(REPO_CACHE_KEY);
    }

    @Override
    public int getAllCount()
    {
        return getAll().size();
    }

    @Override
    public List<Organization> getAllByType(final String dvcsType)
    {
        final List<Organization> orgs = getAll();
        final Iterable<Organization> orgsByType = Iterables.filter(orgs, new Predicate<Organization>()
        {

            @Override
            public boolean apply(@Nullable final Organization org)
            {
                return dvcsType.equals(org.getDvcsType().trim());
            }
        });

        return ImmutableList.copyOf(orgsByType);
    }

    @Override
    public Organization get(final int organizationId)
    {
        final List<Organization> orgs = getAll();

        for (Organization org : orgs)
        {
            if (org.getId() == organizationId)
            {
                return org;
            }
        }
        return null;
    }

    @Override
    public Organization getByHostAndName(final String hostUrl, final String name)
    {
        final List<Organization> orgs = this.getAll();
        for (Organization org : orgs)
        {
            if (hostUrl.equals(org.getHostUrl()) && name.equals(org.getName()))
            {
                return org;
            }
        }
        return null;
    }

    @Override
    public List<Organization> getAllByIds(final Collection<Integer> ids)
    {
        final List<Organization> orgs = getAll();

        final Iterable<Organization> orgsByIds = Iterables.filter(orgs, new Predicate<Organization>()
        {
            @Override
            public boolean apply(@Nullable final Organization org)
            {
                return ids.contains(org.getId());
            }
        });

        return ImmutableList.copyOf(orgsByIds);
    }

    @Override
    public List<Organization> getAutoInvitionOrganizations()
    {
        return organizationDao.getAutoInvitionOrganizations();
    }

    @Override
    public boolean existsOrganizationWithType(final String... types)
    {
        if (ArrayUtils.isEmpty(types))
        {
            return false;
        }

        final List<String> typeList = Arrays.asList(types);

        final List<Organization> orgs = this.getAll();
        for (Organization org : orgs)
        {
            if (typeList.contains(org.getDvcsType()))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public Organization findIntegratedAccount()
    {
        final List<Organization> orgs = this.getAll();
        for (Organization org : orgs)
        {
            if (isNotBlank(org.getCredential().getOauthKey()) && isNotBlank(org.getCredential().getOauthSecret()) &&
                    isBlank(org.getCredential().getAccessToken()))
            {
                return org;
            }
        }
        return null;
    }

    @Override
    public void remove(int organizationId)
    {
        try
        {
            organizationDao.remove(organizationId);
        }
        finally
        {
            clearCache();
        }
    }

    @Override
    public Organization save(final Organization organization)
    {
        try
        {
            return organizationDao.save(organization);
        }
        finally
        {
            clearCache();
        }
    }

    @Override
    public void setDefaultGroupsSlugs(int orgId, Collection<String> groupsSlugs)
    {
        try
        {
            organizationDao.setDefaultGroupsSlugs(orgId, groupsSlugs);
        }
        finally
        {
            clearCache();
        }
    }

    private void clearCache()
    {
        organizationsCache.removeAll();
    }

    private class OrganizationLoader implements CacheLoader<String, List<Organization>>
    {
        @Override
        public List<Organization> load(@Nonnull final String key)
        {
            return organizationDao.getAll();
        }
    }
}
