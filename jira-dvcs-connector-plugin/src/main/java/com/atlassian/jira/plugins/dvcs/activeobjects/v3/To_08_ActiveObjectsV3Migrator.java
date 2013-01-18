package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.ao.Entity;
import net.java.ao.EntityStreamCallback;
import net.java.ao.Query;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.external.ModelVersion;
import com.atlassian.jira.plugins.dvcs.activeobjects.ActiveObjectsUtils;
import com.atlassian.jira.plugins.dvcs.activeobjects.v2.IssueMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v2.ProjectMapping;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.google.common.collect.Maps;

/**
 *  Data migration from jira-github-connector plugin to jira-bitbucket-connector plugin
 *  
 */
@SuppressWarnings("unchecked")
public class To_08_ActiveObjectsV3Migrator implements ActiveObjectsUpgradeTask
{
    private static final Logger log = LoggerFactory.getLogger(To_08_ActiveObjectsV3Migrator.class);
    private static final String ISSUE_KEY_REGEX = "([A-Z]{2,})-\\d+";
    private final PasswordReEncryptor passwordReEncryptor;
  
    public To_08_ActiveObjectsV3Migrator(PasswordReEncryptor passwordReEncryptor)
    {
        this.passwordReEncryptor = passwordReEncryptor;
    }

    @Override
    public void upgrade(ModelVersion currentVersion, ActiveObjects activeObjects)
    {
        log.debug("upgrade [ " + getModelVersion() + " ]");
        
        activeObjects.migrate(ProjectMapping.class, IssueMapping.class, OrganizationMapping.class, RepositoryMapping.class, ChangesetMapping.class);
        
        // BBC-236 this upgrade used to fail midway leaving some inconsistent data in new tables. 
        // We are starting migration again and need to delete those data.
        deleteAllExistingTableContent(activeObjects, ChangesetMapping.class);
        deleteAllExistingTableContent(activeObjects, OrganizationMapping.class);
        deleteAllExistingTableContent(activeObjects, RepositoryMapping.class);
        
        // old repositoryId to new repositoryId
        Map<Integer,Integer> old2New = Maps.newHashMap();
        
        migrateOrganisationsAndRepositories(activeObjects, old2New);
        migrateChangesets(activeObjects,old2New);
    }
    
    private <T extends Entity> void deleteAllExistingTableContent(final ActiveObjects activeObjects, Class<T> entityType)
    {
        ActiveObjectsUtils.delete(activeObjects, entityType, Query.select());
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
     * @param pm
     * @return
     * @throws MalformedURLException 
     */
    private Map<String, Object> createOrganisationMap(ProjectMapping pm) throws MalformedURLException
    {
        URL url = new URL(pm.getRepositoryUrl());
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
        
        String hostUrl = MessageFormat.format("{0}://{1}", protocol, hostname);
        organisationMap.put(OrganizationMapping.HOST_URL, hostUrl);
        organisationMap.put(OrganizationMapping.NAME, owner);
        organisationMap.put(OrganizationMapping.DVCS_TYPE, pm.getRepositoryType());
        organisationMap.put(OrganizationMapping.ADMIN_USERNAME, pm.getAdminUsername());
        organisationMap.put(OrganizationMapping.ADMIN_PASSWORD, 
            reEncryptPassword(pm.getAdminPassword(), pm.getProjectKey(), pm.getRepositoryUrl(), owner, hostUrl));
        organisationMap.put(OrganizationMapping.ACCESS_TOKEN, pm.getAccessToken());
        organisationMap.put(OrganizationMapping.AUTOLINK_NEW_REPOS, false);
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
        
        String name = projectMapping.getRepositoryName()==null?slug:projectMapping.getRepositoryName();

        Map<String, Object> repositoryMap = Maps.newHashMap();
        repositoryMap.put(RepositoryMapping.ORGANIZATION_ID, organisationId);
        repositoryMap.put(RepositoryMapping.SLUG, slug);
        repositoryMap.put(RepositoryMapping.NAME, name);
        repositoryMap.put(RepositoryMapping.LAST_COMMIT_DATE, projectMapping.getLastCommitDate());
        repositoryMap.put(RepositoryMapping.LINKED, true);
        repositoryMap.put(RepositoryMapping.DELETED, false);

        log.debug("Migrating repository : " + repositoryMap);
        return repositoryMap;
    }

    private RepositoryMapping insertRepository(Map<String, Object> repositoryMap, ActiveObjects activeObjects)
    {
        return activeObjects.create(RepositoryMapping.class, repositoryMap);
    }
    
    /**
     * @param activeObjects
     * @param old2New
     */
    private void migrateChangesets(final ActiveObjects activeObjects, final Map<Integer, Integer> old2New)
    {

        activeObjects.stream(IssueMapping.class, new EntityStreamCallback<IssueMapping, Integer>()
        {
            @Override
            public void onRowRead(IssueMapping issueMapping)
            {
                String projectKey = extractProjectKey(issueMapping.getIssueId());
                if (StringUtils.isBlank(projectKey)) 
                {
                    log.warn("Unable to extract project key from issue key: " 
                            + issueMapping.getID() + ", "
                            + issueMapping.getNode() + ", "
                            + issueMapping.getIssueId() + ", "
                            + issueMapping.getMessage()
                            );
                }
                
                Map<String, Object> changesetMap = Maps.newHashMap();
                changesetMap.put(ChangesetMapping.REPOSITORY_ID, old2New.get(issueMapping.getRepositoryId()));
                changesetMap.put(ChangesetMapping.ISSUE_KEY, issueMapping.getIssueId());
                changesetMap.put(ChangesetMapping.PROJECT_KEY, projectKey);
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
        });
    }
    
    
    String extractProjectKey(String issueKey)
    {
        if (StringUtils.isBlank(issueKey))
        {
            return null;
        }
        Pattern projectKeyPattern = Pattern.compile(ISSUE_KEY_REGEX, Pattern.CASE_INSENSITIVE);
        Matcher match = projectKeyPattern.matcher(issueKey);
        if (match.find())
        {
            String projectKey = match.group(1);
            return projectKey;
        }
        return null;
    }

    /**
     * Repositories are no longer associated with the project, hence the encryption using
     * projectKey doesn't work anymore.
     * 
     * @param password
     * @param projectKey
     * @param repositoryUrl 
     * @param organisationName 
     * @param hostUrl 
     * @return
     */
    private String reEncryptPassword(String password, String projectKey, String repositoryUrl, String organisationName, String hostUrl)
    {
        if (password==null)
        {
            return null;
        }
        return passwordReEncryptor.reEncryptPassword(password, projectKey, repositoryUrl, organisationName, hostUrl);
    }

    @Override
    public ModelVersion getModelVersion()
    {
        return ModelVersion.valueOf("8");
    }
}
