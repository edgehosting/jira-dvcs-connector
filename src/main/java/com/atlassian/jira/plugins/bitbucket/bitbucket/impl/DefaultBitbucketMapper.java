package com.atlassian.jira.plugins.bitbucket.bitbucket.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.bitbucket.bitbucket.*;
import com.atlassian.jira.plugins.bitbucket.bitbucket.activeobjects.BitbucketChangesetIssueMapping;
import com.atlassian.jira.plugins.bitbucket.bitbucket.activeobjects.BitbucketRepositoryProjectMapping;
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
                BitbucketRepositoryProjectMapping[] mappings = activeObjects.find(
                        BitbucketRepositoryProjectMapping.class, "projectKey = ?", projectKey);
                List<BitbucketRepository> repositories = new ArrayList<BitbucketRepository>();
                for (BitbucketRepositoryProjectMapping mapping : mappings)
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
        map.put("repositoryOwner", repository.getOwner());
        map.put("repositorySlug", repository.getSlug());
        map.put("projectKey", projectKey);
        if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password))
        {
            map.put("username", username);
            map.put("password", encryptor.encrypt(password, projectKey, repository.getRepositoryUrl()));
        }
        activeObjects.executeInTransaction(new TransactionCallback<Object>()
        {
            public Object doInTransaction()
            {
                activeObjects.create(BitbucketRepositoryProjectMapping.class, map);
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
                BitbucketRepositoryProjectMapping[] projectMappings = activeObjects.find(BitbucketRepositoryProjectMapping.class,
                        "projectKey = ? and repositoryOwner = ? and repositorySlug = ?",
                        projectKey, repository.getOwner(), repository.getSlug());
                for (BitbucketRepositoryProjectMapping mapping : projectMappings)
                    activeObjects.delete(mapping);

                BitbucketChangesetIssueMapping[] changesetMappings = activeObjects.find(BitbucketChangesetIssueMapping.class,
                        "projectKey = ? and repositoryOwner = ? and repositorySlug = ?",
                        projectKey, repository.getOwner(), repository.getSlug());
                for (BitbucketChangesetIssueMapping mapping : changesetMappings)
                    activeObjects.delete(mapping);
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

                BitbucketChangesetIssueMapping[] mappings = activeObjects.find(
                        BitbucketChangesetIssueMapping.class, "issueId = ?", issueId);
                List<BitbucketChangeset> changesets = new ArrayList<BitbucketChangeset>();
                for (BitbucketChangesetIssueMapping mapping : mappings)
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
        map.put("projectKey", getProjectKey(issueId));
        map.put("issueId", issueId);
        map.put("repositoryOwner", changeset.getRepositoryOwner());
        map.put("repositorySlug", changeset.getRepositorySlug());
        activeObjects.executeInTransaction(new TransactionCallback<Object>()
        {
            public Object doInTransaction()
            {
                activeObjects.create(BitbucketChangesetIssueMapping.class, map);
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
                BitbucketChangesetIssueMapping[] mappings = activeObjects.find(BitbucketChangesetIssueMapping.class,
                        "issueId = ? and node = ?",
                        issueId, changeset.getNode());
                for (BitbucketChangesetIssueMapping mapping : mappings)
                    activeObjects.delete(mapping);
                return null;
            }
        });
    }

    private BitbucketAuthentication getAuthentication(String projectKey, String owner, String slug)
    {
        BitbucketRepositoryProjectMapping[] projectRepositoryMappings = activeObjects.find(BitbucketRepositoryProjectMapping.class,
                "projectKey = ? and repositoryOwner = ? and repositorySlug = ?",
                projectKey, owner, slug);
        if (projectRepositoryMappings == null || projectRepositoryMappings.length != 1)
            throw new BitbucketException("invalid mapping for project [ " + projectKey + " ] to " +
                    "repository [ " + owner + "/" + slug + " ] was [ " +
                    (projectRepositoryMappings == null ? "null" : String.valueOf(projectRepositoryMappings.length)) + " ]");
        return getAuthentication(projectRepositoryMappings[0]);
    }

    private String getProjectKey(String issueId)
    {
        // TODO is this safe to do?
        return issueId.substring(0, issueId.lastIndexOf("-"));
    }

    private BitbucketAuthentication getAuthentication(BitbucketRepositoryProjectMapping mapping)
    {
        BitbucketAuthentication auth = BitbucketAuthentication.ANONYMOUS;
        if (StringUtils.isNotBlank(mapping.getUsername()) && StringUtils.isNotBlank(mapping.getPassword()))
            auth = BitbucketAuthentication.basic(mapping.getUsername(), mapping.getPassword());
        return auth;
    }

}
