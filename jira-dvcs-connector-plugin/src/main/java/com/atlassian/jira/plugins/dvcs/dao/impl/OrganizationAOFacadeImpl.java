package com.atlassian.jira.plugins.dvcs.dao.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.OrganizationMapping;
import com.atlassian.jira.plugins.dvcs.crypto.Encryptor;
import com.atlassian.jira.plugins.dvcs.dao.OrganizationAOFacade;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Group;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.InvalidOrganizationManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import net.java.ao.Query;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

@Component ("organizationAOFacade")
public class OrganizationAOFacadeImpl implements OrganizationAOFacade
{
    public static final Logger log = LoggerFactory.getLogger(OrganizationAOFacadeImpl.class);

    private final ActiveObjects activeObjects;
    private final Encryptor encryptor;
    private final InvalidOrganizationManager invalidOrganizationsManager;

    @Autowired
    public OrganizationAOFacadeImpl(@ComponentImport ActiveObjects activeObjects, Encryptor encryptor,
            InvalidOrganizationManager invalidOrganizationsManager)
    {
        this.activeObjects = checkNotNull(activeObjects);
        this.encryptor = encryptor;
        this.invalidOrganizationsManager = checkNotNull(invalidOrganizationsManager);
    }

    /**
     * Transform.
     *
     * @param organizationMapping the organization mapping
     * @return the organization
     */
    protected Organization transform(OrganizationMapping organizationMapping)
    {

        if (organizationMapping == null)
        {
            return null;
        }

        // make credentials
        Credential credential = new Credential(
                organizationMapping.getOauthKey(), organizationMapping.getOauthSecret(),
                organizationMapping.getAccessToken(),
                organizationMapping.getAdminUsername(), organizationMapping.getAdminPassword());
        //
        Organization organization = new Organization(organizationMapping.getID(), organizationMapping.getHostUrl(),
                organizationMapping.getName(), organizationMapping.getDvcsType(),
                organizationMapping.isAutolinkNewRepos(), credential, createOrganizationUrl(organizationMapping),
                organizationMapping.isSmartcommitsForNewRepos(), deserializeDefaultGroups(organizationMapping.getDefaultGroupsSlugs()));
        return organization;
    }

    protected Set<Group> deserializeDefaultGroups(String defaultGroupsSlugs)
    {
        Set<Group> slugs = new HashSet<Group>();
        if (StringUtils.isNotBlank(defaultGroupsSlugs))
        {
            Iterable<String> groupsSlugs = Splitter.on(Organization.GROUP_SLUGS_SEPARATOR).split(defaultGroupsSlugs);
            for (String slug : groupsSlugs)
            {
                if (StringUtils.isNotBlank(slug))
                {
                    slugs.add(new Group(StringUtils.trim(slug)));
                }
            }
        }
        return slugs;
    }

    protected String serializeDefaultGroups(Set<Group> groups)
    {
        if (CollectionUtils.isEmpty(groups))
        {
            return "";
        }
        return Joiner.on(Organization.GROUP_SLUGS_SEPARATOR).join(groups);
    }

    private String createOrganizationUrl(OrganizationMapping organizationMapping)
    {
        String hostUrl = organizationMapping.getHostUrl();
        // normalize
        if (hostUrl != null && hostUrl.endsWith("/"))
        {
            hostUrl = hostUrl.substring(0, hostUrl.length() - 1);
        }
        return hostUrl + "/" + organizationMapping.getName();
    }

    @Override
    public List<Organization> fetch()
    {
        final List<OrganizationMapping> organizationMappings = activeObjects
                .executeInTransaction(new TransactionCallback<List<OrganizationMapping>>()
                {

                    @Override
                    public List<OrganizationMapping> doInTransaction()
                    {
                        return Lists.newArrayList(activeObjects.find(OrganizationMapping.class, Query.select().order(OrganizationMapping.NAME)));
                    }
                });

        return transformCollection(organizationMappings);
    }

    /**
     * Transform collection.
     *
     * @param organizationMappings the organization mappings
     * @return the list< organization>
     */
    @SuppressWarnings ("unchecked")
    private List<Organization> transformCollection(List<OrganizationMapping> organizationMappings)
    {
        return (List<Organization>) CollectionUtils.collect(organizationMappings, new Transformer()
        {

            @Override
            public Object transform(Object input)
            {
                OrganizationMapping organizationMapping = (OrganizationMapping) input;

                return OrganizationAOFacadeImpl.this.transform(organizationMapping);
            }
        });
    }

    @Override
    public Organization save(final Organization organization)
    {

        final OrganizationMapping organizationMapping = activeObjects
                .executeInTransaction(new TransactionCallback<OrganizationMapping>()
                {

                    @Override
                    public OrganizationMapping doInTransaction()
                    {
                        String adminPassword = organization.getCredential().getAdminPassword();

                        OrganizationMapping om = null;

                        if (organization.getId() == 0)
                        {
                            // encrypt password for new organization
                            if (adminPassword != null)
                            {
                                adminPassword = encryptor.encrypt(organization.getCredential().getAdminPassword(),
                                        organization.getName(), organization.getHostUrl());
                            }

                            // we need to remove null characters '\u0000' because PostgreSQL cannot store String values
                            // with such characters
                            final Map<String, Object> map = new MapRemovingNullCharacterFromStringValues();
                            map.put(OrganizationMapping.HOST_URL, organization.getHostUrl());
                            map.put(OrganizationMapping.NAME, organization.getName());
                            map.put(OrganizationMapping.DVCS_TYPE, organization.getDvcsType());
                            map.put(OrganizationMapping.AUTOLINK_NEW_REPOS, organization.isAutolinkNewRepos());
                            map.put(OrganizationMapping.ADMIN_USERNAME, organization.getCredential().getAdminUsername());
                            map.put(OrganizationMapping.ADMIN_PASSWORD, adminPassword);
                            map.put(OrganizationMapping.ACCESS_TOKEN, organization.getCredential().getAccessToken());
                            map.put(OrganizationMapping.SMARTCOMMITS_FOR_NEW_REPOS, organization.isSmartcommitsOnNewRepos());
                            map.put(OrganizationMapping.DEFAULT_GROUPS_SLUGS, serializeDefaultGroups(organization.getDefaultGroups()));

                            map.put(OrganizationMapping.OAUTH_KEY, organization.getCredential().getOauthKey());
                            map.put(OrganizationMapping.OAUTH_SECRET, organization.getCredential().getOauthSecret());

                            om = activeObjects.create(OrganizationMapping.class, map);
                            om = activeObjects.find(OrganizationMapping.class, "ID = ?", om.getID())[0];
                        }
                        else
                        {
                            om = activeObjects.get(OrganizationMapping.class, organization.getId());

                            om.setHostUrl(organization.getHostUrl());
                            om.setName(organization.getName());
                            om.setDvcsType(organization.getDvcsType());
                            om.setAutolinkNewRepos(organization.isAutolinkNewRepos());
                            om.setSmartcommitsForNewRepos(organization.isSmartcommitsOnNewRepos());
                            om.setDefaultGroupsSlugs(serializeDefaultGroups(organization.getDefaultGroups()));

                            om.setAdminUsername(organization.getCredential().getAdminUsername());
                            om.setAdminPassword(adminPassword);
                            om.setAccessToken(organization.getCredential().getAccessToken());
                            om.setOauthKey(organization.getCredential().getOauthKey());
                            om.setOauthSecret(organization.getCredential().getOauthSecret());

                            om.save();
                        }

                        return om;
                    }
                });

        return transform(organizationMapping);
    }

    @Override
    public void remove(int organizationId)
    {
        OrganizationMapping organizationMapping = activeObjects.get(OrganizationMapping.class, organizationId);
        if (organizationMapping != null)
        {
            activeObjects.delete(organizationMapping);
        }

        // removing organization from invalid organizations list
        invalidOrganizationsManager.setOrganizationValid(organizationId, true);
    }

    @Override
    public void updateDefaultGroupsSlugs(int orgId, Collection<String> groupsSlugs)
    {
        String serializedGroups = null;
        if (CollectionUtils.isNotEmpty(groupsSlugs))
        {
            serializedGroups = Joiner.on(Organization.GROUP_SLUGS_SEPARATOR).join(groupsSlugs);
        }

        final OrganizationMapping organization = activeObjects.get(OrganizationMapping.class, orgId);
        organization.setDefaultGroupsSlugs(serializedGroups);
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {
            @Override
            public Void doInTransaction()
            {
                organization.save();
                return null;
            }
        });
    }
}
