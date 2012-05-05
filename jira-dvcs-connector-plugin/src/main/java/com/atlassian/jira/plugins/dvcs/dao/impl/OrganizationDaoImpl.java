package com.atlassian.jira.plugins.dvcs.dao.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.OrganizationMapping;
import com.atlassian.jira.plugins.dvcs.crypto.Encryptor;
import com.atlassian.jira.plugins.dvcs.dao.OrganizationDao;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.google.common.collect.Lists;
import net.java.ao.Query;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrganizationDaoImpl implements OrganizationDao
{

    private final ActiveObjects activeObjects;
    private final Encryptor encryptor;


    public OrganizationDaoImpl(ActiveObjects activeObjects, Encryptor encryptor)
    {
        this.activeObjects = activeObjects;
        this.encryptor = encryptor;
    }

    protected Organization transform(OrganizationMapping organizationMapping) {

        String decryptedPasswd = encryptor.decrypt(organizationMapping.getAdminPassword(),
                organizationMapping.getName(),
                organizationMapping.getHostUrl());

        Credential credential = new Credential(organizationMapping.getAdminUsername(),
                decryptedPasswd,
                organizationMapping.getAccessToken());

        Organization organization = new Organization(organizationMapping.getID(),
                organizationMapping.getHostUrl(),
                organizationMapping.getName(),
                organizationMapping.getDvcsType(),
                organizationMapping.isAutolinkNewRepos(),
                credential);
        return organization;
    }


    @Override
    @SuppressWarnings("unchecked")
    public List<Organization> getAll()
    {
        final List<OrganizationMapping> organizationMappings = activeObjects.executeInTransaction(new TransactionCallback<List<OrganizationMapping>>()
        {

            @Override
            public List<OrganizationMapping> doInTransaction()
            {
                return Lists.newArrayList(activeObjects.find(OrganizationMapping.class));
            }
        });

        List<Organization> organizations = (List<Organization>) CollectionUtils.collect(organizationMappings, new Transformer()
        {
            @Override
            public Object transform(Object o)
            {
                return OrganizationDaoImpl.this.transform((OrganizationMapping) o);
            }
        });


        return organizations;
    }



    @Override
    public Organization get(final int organizationId)
    {
        OrganizationMapping organizationMapping = activeObjects.executeInTransaction(new TransactionCallback<OrganizationMapping>()
        {
            @Override
            public OrganizationMapping doInTransaction()
            {
                return activeObjects.get(OrganizationMapping.class, organizationId);
            }
        });


        return transform(organizationMapping);
    }

    @Override
    public Organization getByHostAndName(final String hostUrl, final String name)
    {
        OrganizationMapping organizationMapping = activeObjects.executeInTransaction(new TransactionCallback<OrganizationMapping>()
        {
            @Override
            public OrganizationMapping doInTransaction()
            {
                Query query = Query.select().where(OrganizationMapping.HOST_URL + " = ? AND " +
                            OrganizationMapping.NAME + " = ? ", hostUrl, name);

                final OrganizationMapping[] organizationMappings = activeObjects.find(OrganizationMapping.class, query);
                return organizationMappings.length != 0 ? organizationMappings[0] : null;
            }
        });


        return transform(organizationMapping);

    }

    @Override
    public Organization save(final Organization organization)
    {
        final String encryptedPasswd = encryptor.encrypt(organization.getCredential().getAdminPassword(),
                organization.getName(),
                organization.getHostUrl());


        final OrganizationMapping organizationMapping = activeObjects.executeInTransaction(new TransactionCallback<OrganizationMapping>()
        {

            @Override
            public OrganizationMapping doInTransaction()
            {
                OrganizationMapping om;
                if (organization.getId() == 0)
                {
                    final Map<String, Object> map = new HashMap<String, Object>();
                    map.put(OrganizationMapping.HOST_URL, organization.getHostUrl());
                    map.put(OrganizationMapping.NAME, organization.getName());
                    map.put(OrganizationMapping.DVCS_TYPE, organization.getDvcsType());
                    map.put(OrganizationMapping.AUTOLINK_NEW_REPOS, organization.isAutolinkNewRepos());
                    map.put(OrganizationMapping.ADMIN_USERNAME, organization.getCredential().getAdminUsername());
                    map.put(OrganizationMapping.ADMIN_PASSWORD, encryptedPasswd);
                    map.put(OrganizationMapping.ACCESS_TOKEN, organization.getCredential().getAccessToken());

                    om = activeObjects.create(OrganizationMapping.class, map);
                } else {
                    om = activeObjects.get(OrganizationMapping.class, organization.getId());

                    om.setHostUrl(organization.getHostUrl());
                    om.setName(organization.getName());
                    om.setDvcsType(organization.getDvcsType());
                    om.setAutolinkNewRepos(organization.isAutolinkNewRepos());
                    om.setAdminUsername(organization.getCredential().getAdminUsername());
                    om.setAdminPassword(encryptedPasswd);
                    om.setAccessToken(organization.getCredential().getAccessToken());

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
    }
}
