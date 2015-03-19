package com.atlassian.jira.plugins.dvcs.dao.impl;

import com.atlassian.cache.CacheManager;
import com.atlassian.cache.CacheSettings;
import com.atlassian.cache.CacheSettingsBuilder;
import com.atlassian.cache.CachedReference;
import com.atlassian.cache.Supplier;
import com.atlassian.jira.cluster.ClusterSafe;
import com.atlassian.jira.plugins.dvcs.dao.OrganizationAOFacade;
import com.atlassian.jira.plugins.dvcs.dao.OrganizationDao;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.google.common.base.Function;
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

import static com.atlassian.gzipfilter.org.apache.commons.lang.StringUtils.isBlank;
import static com.atlassian.gzipfilter.org.apache.commons.lang.StringUtils.isNotBlank;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * Cache Organization and return defensive copies rather than the cached object. Cache expires according to
 * CACHE_SETTINGS configuration.
 */
@Component ("organizationDao")
public class CachingOrganizationDaoImpl implements OrganizationDao
{
    private static final CacheSettings CACHE_SETTINGS = new CacheSettingsBuilder().expireAfterWrite(30, MINUTES).build();

    @ClusterSafe
    private final CachedReference<List<Organization>> organizationsCache;

    @Autowired
    @Qualifier ("organizationAOFacade")
    private OrganizationAOFacade organizationAOFacade;

    @Autowired
    public CachingOrganizationDaoImpl(@ComponentImport final CacheManager cacheManager)
    {
        organizationsCache = cacheManager.getCachedReference(getClass().getName() + ".organizationsCache", new Supplier<List<Organization>>()
        {
            @Override
            public List<Organization> get()
            {
                return organizationAOFacade.fetch();
            }
        }, CACHE_SETTINGS);
    }

    @Override
    public List<Organization> getAll()
    {
        return cloneOrgs(getAllCachedOrgs());
    }

    @Override
    public int getAllCount()
    {
        return getAllCachedOrgs().size();
    }

    @Override
    public List<Organization> getAllByType(final String dvcsType)
    {
        final List<Organization> orgs = getAllCachedOrgs();
        final Iterable<Organization> orgsByType = Iterables.filter(orgs, new Predicate<Organization>()
        {

            @Override
            public boolean apply(@Nonnull final Organization org)
            {
                return dvcsType.equals(org.getDvcsType());
            }
        });
        return cloneOrgs(orgsByType);
    }

    @Override
    public Organization get(final int organizationId)
    {
        return findOrganization(new Predicate<Organization>()
        {
            @Override
            public boolean apply(@Nonnull final Organization org)
            {
                return org.getId() == organizationId;
            }
        });
    }

    @Override
    public Organization getByHostAndName(final String hostUrl, final String name)
    {
        checkNotNull(hostUrl);
        checkNotNull(name);

        return findOrganization(new Predicate<Organization>()
        {
            @Override
            public boolean apply(@Nonnull final Organization org)
            {
                return hostUrl.equals(org.getHostUrl()) && name.equalsIgnoreCase(org.getName());
            }
        });
    }

    @Override
    public List<Organization> getAllByIds(final Collection<Integer> ids)
    {
        checkNotNull(ids);

        final List<Organization> orgs = getAllCachedOrgs();

        final Iterable<Organization> orgsByIds = Iterables.filter(orgs, new Predicate<Organization>()
        {
            @Override
            public boolean apply(@Nonnull final Organization org)
            {
                return ids.contains(org.getId());
            }
        });

        return cloneOrgs(orgsByIds);
    }

    @Override
    public boolean existsOrganizationWithType(final String... types)
    {
        if (types == null || ArrayUtils.isEmpty(types))
        {
            return false;
        }

        final List<String> typeList = Arrays.asList(types);
        Organization org = findOrganization(new Predicate<Organization>()
        {
            @Override
            public boolean apply(@Nonnull final Organization org)
            {
                return typeList.contains(org.getDvcsType());
            }
        });

        return org != null;
    }

    @Override
    public Organization findIntegratedAccount()
    {
        return findOrganization(new Predicate<Organization>()
        {
            @Override
            public boolean apply(@Nonnull final Organization org)
            {
                return isNotBlank(org.getCredential().getOauthKey()) && isNotBlank(org.getCredential().getOauthSecret()) &&
                        isBlank(org.getCredential().getAccessToken());
            }
        });
    }

    @Override
    public void remove(int organizationId)
    {
        organizationAOFacade.remove(organizationId);
        // if operation succeeds then clear the cache otherwise do not clear the cache to keep some stale data for the users
        clearCache();
    }

    @Override
    public Organization save(final Organization organization)
    {
        Organization org = organizationAOFacade.save(organization);

        // if operation succeeds then clear the cache otherwise do not clear the cache to keep some stale data for the users
        if (org != null)
        {
            clearCache();
        }
        return org;
    }

    @Override
    public void setDefaultGroupsSlugs(int orgId, Collection<String> groupsSlugs)
    {
        organizationAOFacade.updateDefaultGroupsSlugs(orgId, groupsSlugs);

        // if operation succeeds then clear the cache otherwise do not clear the cache to keep some stale data for the users
        clearCache();
    }

    private List<Organization> getAllCachedOrgs()
    {
        return organizationsCache.get();
    }

    private Organization findOrganization(Predicate<Organization> predicate)
    {
        final List<Organization> orgs = getAllCachedOrgs();
        Organization matchedOrg = Iterables.find(orgs, predicate, null);
        return matchedOrg != null ? new Organization(matchedOrg) : null;
    }

    private List<Organization> cloneOrgs(@Nonnull Iterable<Organization> orgs)
    {
        return ImmutableList.copyOf(Iterables.transform(orgs, new Function<Organization, Organization>()
        {
            @Override
            public Organization apply(@Nonnull Organization org)
            {
                return new Organization(org);
            }
        }));
    }

    private void clearCache()
    {
        organizationsCache.reset();
    }
}
