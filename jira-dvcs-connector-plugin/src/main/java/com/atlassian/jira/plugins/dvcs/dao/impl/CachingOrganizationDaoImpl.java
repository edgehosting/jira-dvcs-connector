package com.atlassian.jira.plugins.dvcs.dao.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheLoader;
import com.atlassian.cache.CacheManager;
import com.atlassian.cache.CacheSettings;
import com.atlassian.cache.CacheSettingsBuilder;
import com.atlassian.jira.plugins.dvcs.crypto.Encryptor;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.InvalidOrganizationManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.apache.commons.lang.ArrayUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import static com.atlassian.gzipfilter.org.apache.commons.lang.StringUtils.isBlank;
import static com.atlassian.gzipfilter.org.apache.commons.lang.StringUtils.isNotBlank;
import static java.util.concurrent.TimeUnit.MINUTES;

@Named ("cachingOrganizationDao")
public class CachingOrganizationDaoImpl extends OrganizationDaoImpl
{
    private static final CacheSettings CACHE_SETTINGS = new CacheSettingsBuilder().expireAfterWrite(30, MINUTES).build();
    private Cache<String, List<Organization>> organizationsCache;

    @VisibleForTesting
    static final String REPO_CACHE_KEY = "all";

    @Inject
    public CachingOrganizationDaoImpl(@ComponentImport ActiveObjects activeObjects,
            @ComponentImport final CacheManager cacheManager,
            Encryptor encryptor,
            InvalidOrganizationManager invalidOrganizationsManager)
    {
        super(activeObjects, encryptor, invalidOrganizationsManager);
        organizationsCache = cacheManager.getCache(getClass().getName() + ".organizationsCache", new OrganizationLoader(), CACHE_SETTINGS);
    }

    @Override
    public List<Organization> getAll()
    {
        return organizationsCache.get(REPO_CACHE_KEY);
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
            super.remove(organizationId);
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
            return super.save(organization);
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
            super.setDefaultGroupsSlugs(orgId, groupsSlugs);
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

    private List<Organization> getAllFromSuper()
    {
        return super.getAll();
    }

    private class OrganizationLoader implements CacheLoader<String, List<Organization>>
    {
        @Override
        public List<Organization> load(@Nonnull final String key)
        {
            return getAllFromSuper();
        }
    }
}
