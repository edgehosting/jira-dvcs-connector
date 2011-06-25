package com.atlassian.jira.plugins.bitbucket.bitbucket.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.bitbucket.bitbucket.*;
import com.atlassian.jira.plugins.bitbucket.bitbucket.activeobjects.IssueMapping;
import com.atlassian.jira.plugins.bitbucket.bitbucket.activeobjects.ProjectMapping;
import com.atlassian.sal.api.transaction.TransactionCallback;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple bitbucket mapper that uses ActiveObjects to store the mapping details
 */
public class DefaultBitbucketMapper implements BitbucketMapper
{
    private final ActiveObjects activeObjects;
    private final Bitbucket bitbucket;
    private final Encryptor encryptor;

    public DefaultBitbucketMapper(ActiveObjects activeObjects, Bitbucket bitbucket, Encryptor encryptor)
    {
        this.activeObjects = activeObjects;
        this.bitbucket = bitbucket;
        this.encryptor = encryptor;
    }

    public List<BitbucketRepository> getRepositories(final String projectKey)
    {
        return activeObjects.executeInTransaction(new TransactionCallback<List<BitbucketRepository>>()
        {
            public List<BitbucketRepository> doInTransaction()
            {
                ProjectMapping[] mappings = activeObjects.find(
                        ProjectMapping.class, "project_key = ?", projectKey);
                List<BitbucketRepository> repositories = new ArrayList<BitbucketRepository>();
                for (ProjectMapping mapping : mappings)
                {
                    BitbucketAuthentication auth = getAuthentication(mapping);
                    repositories.add(BitbucketRepositoryFactory.load(bitbucket, auth, mapping.getRepositoryOwner(), mapping.getRepositorySlug()));
                }
                return repositories;
            }
        });
    }

    public void addRepository(String projectKey, BitbucketRepository repository, String username, String password)
    {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("repository_owner", repository.getOwner());
        map.put("repository_slug", repository.getSlug());
        map.put("project_key", projectKey);
        if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password))
        {
            map.put("username", username);
            map.put("password", encryptor.encrypt(password, projectKey, repository.getRepositoryUrl()));
        }
        activeObjects.executeInTransaction(new TransactionCallback<Object>()
        {
            public Object doInTransaction()
            {
                activeObjects.create(ProjectMapping.class, map);
                return null;
            }
        });
    }

    public void removeRepository(final String projectKey, final BitbucketRepository repository)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Object>()
        {
            public Object doInTransaction()
            {
                ProjectMapping[] projectMappings = activeObjects.find(ProjectMapping.class,
                        "project_key = ? and repository_owner = ? and repository_slug = ?",
                        projectKey, repository.getOwner(), repository.getSlug());
                activeObjects.delete(projectMappings);

                IssueMapping[] mappings = activeObjects.find(IssueMapping.class,
                        "project_key = ? and repository_owner = ? and repository_slug = ?",
                        projectKey, repository.getOwner(), repository.getSlug());
                activeObjects.delete(mappings);
                return null;
            }
        });
    }

    public List<BitbucketChangeset> getChangesets(final String issueId)
    {
        return activeObjects.executeInTransaction(new TransactionCallback<List<BitbucketChangeset>>()
        {
            public List<BitbucketChangeset> doInTransaction()
            {
                String projectKey = getProjectKey(issueId);

                IssueMapping[] mappings = activeObjects.find(
                        IssueMapping.class, "issue_id = ?", issueId);
                List<BitbucketChangeset> changesets = new ArrayList<BitbucketChangeset>();
                for (IssueMapping mapping : mappings)
                {
                    String owner = mapping.getRepositoryOwner();
                    String slug = mapping.getRepositorySlug();
                    BitbucketAuthentication auth = getAuthentication(projectKey, owner, slug);
                    changesets.add(BitbucketChangesetFactory.load(bitbucket, auth, owner, slug, mapping.getNode()));
                }
                return changesets;
            }
        });
    }

    public void addChangeset(String issueId, BitbucketChangeset changeset)
    {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("node", changeset.getNode());
        map.put("project_key", getProjectKey(issueId));
        map.put("issue_id", issueId);
        map.put("repository_owner", changeset.getRepositoryOwner());
        map.put("repository_slug", changeset.getRepositorySlug());
        activeObjects.executeInTransaction(new TransactionCallback<Object>()
        {
            public Object doInTransaction()
            {
                activeObjects.create(IssueMapping.class, map);
                return null;
            }
        });
    }

    public void removeChangeset(final String issueId, final BitbucketChangeset changeset)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Object>()
        {
            public Object doInTransaction()
            {
                IssueMapping[] mappings = activeObjects.find(IssueMapping.class,
                        "issue_id = ? and node = ?",
                        issueId, changeset.getNode());
                activeObjects.delete(mappings);
                return null;
            }
        });
    }

    private BitbucketAuthentication getAuthentication(String projectKey, String owner, String slug)
    {
        ProjectMapping[] projectMappings = activeObjects.find(ProjectMapping.class,
                "project_key = ? and repository_owner = ? and repository_slug = ?",
                projectKey, owner, slug);
        if (projectMappings == null || projectMappings.length != 1)
            throw new BitbucketException("invalid mapping for project [ " + projectKey + " ] to " +
                    "repository [ " + owner + "/" + slug + " ] was [ " +
                    (projectMappings == null ? "null" : String.valueOf(projectMappings.length)) + " ]");
        return getAuthentication(projectMappings[0]);
    }

    private String getProjectKey(String issueId)
    {
        // TODO is this safe to do?
        return issueId.substring(0, issueId.lastIndexOf("-"));
    }

    private BitbucketAuthentication getAuthentication(ProjectMapping mapping)
    {
        BitbucketAuthentication auth = BitbucketAuthentication.ANONYMOUS;
        if (StringUtils.isNotBlank(mapping.getUsername()) && StringUtils.isNotBlank(mapping.getPassword()))
            auth = BitbucketAuthentication.basic(mapping.getUsername(), mapping.getPassword());
        return auth;
    }

}
