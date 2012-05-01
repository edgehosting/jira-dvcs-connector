package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.external.ModelVersion;
import com.atlassian.jira.plugins.bitbucket.activeobjects.v2.IssueMapping;
import com.atlassian.jira.plugins.bitbucket.activeobjects.v2.ProjectMapping;
import com.atlassian.jira.plugins.bitbucket.api.exception.SourceControlException;
import com.google.common.collect.Maps;

/**
 *  Data migration from jira-github-connector plugin to jira-bitbucket-connector plugin
 *  
 */
@SuppressWarnings("unchecked")
public class To_08_ActiveObjectsV3Migrator implements ActiveObjectsUpgradeTask
{
    private static final Logger log = LoggerFactory.getLogger(To_08_ActiveObjectsV3Migrator.class);
  
    @Override
    public void upgrade(ModelVersion currentVersion, ActiveObjects activeObjects)
    {
        log.debug("upgrade [ " + getModelVersion() + " ]");
        
        activeObjects.migrate(ProjectMapping.class, IssueMapping.class, OrganizationMapping.class, RepositoryMapping.class, ChangesetMapping.class);
        
        // old repositoryId to new repositoryId
        Map<Integer,Integer> old2New = Maps.newHashMap();
        
        migrateOrganisationsAndRepositories(activeObjects, old2New);
        migrateChangesets(activeObjects,old2New);
    }

    private void migrateOrganisationsAndRepositories(ActiveObjects activeObjects, Map<Integer, Integer> old2New)
    {
        ProjectMapping[] ProjectMappings = activeObjects.find(ProjectMapping.class);
        for (ProjectMapping projectMapping : ProjectMappings)
        {
            try 
            {
                // add organisation
                Map<String, Object> organisationData = createOrganisationMap(projectMapping);
                OrganizationMapping organisationMapping = insertOrganisation(organisationData, activeObjects);
                
                // add repository 
                Map<String,Object> repositoryData = createRepositoryMap(organisationMapping.getID(), projectMapping);
                RepositoryMapping repositoryMapping = insertRepository(repositoryData, activeObjects);
                
                // remember new repository ID
                old2New.put(projectMapping.getID(), repositoryMapping.getID());
            } catch (Exception e)
            {
                log.error("Error during DVCS data migration: "+ e.getMessage(), e);
            }
        }
    }

    /**
     * Copied from DvcsRepositoryManager#parseRepositoryUri
     * 
     * @param projectMapping
     * @return
     * @throws MalformedURLException 
     */
    private Map<String, Object> createOrganisationMap(ProjectMapping projectMapping) throws MalformedURLException
    {
        URL url = new URL(projectMapping.getRepositoryUrl());
        String protocol = url.getProtocol();
        String hostname = url.getHost();
        String path = url.getPath();
        String[] split = StringUtils.split(path, "/");
        if (split.length < 2)
        {
            throw new SourceControlException("Expected url is https://domainname.com/username/repository");
        }
        String owner = split[0];
        
        Map<String, Object> organisationMap = Maps.newHashMap();
        
        organisationMap.put(OrganizationMapping.HOST_URL, MessageFormat.format("{0}://{1}", protocol, hostname));
        organisationMap.put(OrganizationMapping.NAME, owner);
        organisationMap.put(OrganizationMapping.DVCS_TYPE, projectMapping.getRepositoryType());
        organisationMap.put(OrganizationMapping.ADMIN_USERNAME, projectMapping.getAdminUsername());
        organisationMap.put(OrganizationMapping.ADMIN_PASSWORD, projectMapping.getAdminPassword());
        organisationMap.put(OrganizationMapping.ACCESS_TOKEN, projectMapping.getAccessToken());
        // TODO - set autolinking to false;
        return organisationMap;
    }

    private OrganizationMapping insertOrganisation(Map<String, Object> organisationMap, ActiveObjects activeObjects)
    {
        String hostUrl = (String) organisationMap.get(OrganizationMapping.HOST_URL);
        String name = (String) organisationMap.get(OrganizationMapping.NAME);
        
        OrganizationMapping[] existing = activeObjects.find(OrganizationMapping.class, 
            OrganizationMapping.HOST_URL+" = ? and " + OrganizationMapping.NAME + " = ?", hostUrl, name);
        
        if (existing.length>0)
        {
            return existing[0];
        }
        log.debug("Adding new organisation: " + organisationMap);
        return activeObjects.create(OrganizationMapping.class, organisationMap);
    }

    private Map<String, Object> createRepositoryMap(int organisationId, ProjectMapping projectMapping) throws MalformedURLException
    {
        URL url = new URL(projectMapping.getRepositoryUrl());
        String path = url.getPath();
        String[] split = StringUtils.split(path, "/");
        if (split.length < 2)
        {
            throw new SourceControlException("Expected url is https://domainname.com/username/repository");
        }
        String slug = split[1];

        Map<String, Object> repositoryMap = Maps.newHashMap();
        repositoryMap.put(RepositoryMapping.ORGANIZATION_ID, organisationId);
        repositoryMap.put(RepositoryMapping.SLUG, slug);
        repositoryMap.put(RepositoryMapping.NAME, projectMapping.getRepositoryName());
        repositoryMap.put(RepositoryMapping.LAST_COMMIT_DATE, projectMapping.getLastCommitDate());
        repositoryMap.put(RepositoryMapping.LINKED, true);
        log.debug("Migrating repository : " + repositoryMap);
        return repositoryMap;
    }

    private RepositoryMapping insertRepository(Map<String, Object> repositoryMap, ActiveObjects activeObjects)
    {
        return activeObjects.create(RepositoryMapping.class, repositoryMap);
    }
    
    /**
     * TODO check with Sam efficiency of this method
     * @param activeObjects
     * @param old2New
     */
    private void migrateChangesets(ActiveObjects activeObjects, Map<Integer, Integer> old2New)
    {
        IssueMapping[] issueMappings = activeObjects.find(IssueMapping.class);
        for (IssueMapping issueMapping : issueMappings)
        {
            Map<String, Object> changesetMap = Maps.newHashMap();
            changesetMap.put(ChangesetMapping.REPOSITORY_ID, old2New.get(issueMapping.getRepositoryId()));
            changesetMap.put(ChangesetMapping.ISSUE_ID, issueMapping.getIssueId());
            changesetMap.put(ChangesetMapping.NODE, issueMapping.getNode());
            changesetMap.put(ChangesetMapping.RAW_AUTHOR, issueMapping.getRawAuthor());
            changesetMap.put(ChangesetMapping.AUTHOR, issueMapping.getAuthor());
            changesetMap.put(ChangesetMapping.DATE, issueMapping.getDate());
            changesetMap.put(ChangesetMapping.RAW_NODE, issueMapping.getRawNode());
            changesetMap.put(ChangesetMapping.BRANCH, issueMapping.getBranch());
            changesetMap.put(ChangesetMapping.MESSAGE, issueMapping.getMessage());
            changesetMap.put(ChangesetMapping.PARENTS_DATA, issueMapping.getParentsData());
            changesetMap.put(ChangesetMapping.FILES_DATA, issueMapping.getFilesData());
            changesetMap.put(ChangesetMapping.VERSION, issueMapping.getVersion());
            activeObjects.create(ChangesetMapping.class, changesetMap);
        }
    }

    @Override
    public ModelVersion getModelVersion()
    {
        return ModelVersion.valueOf("8");
    }
}
