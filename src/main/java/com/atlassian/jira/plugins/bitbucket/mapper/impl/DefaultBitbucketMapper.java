package com.atlassian.jira.plugins.bitbucket.mapper.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.bitbucket.activeobjects.v1.IssueMapping;
import com.atlassian.jira.plugins.bitbucket.activeobjects.v1.ProjectMapping;
import com.atlassian.jira.plugins.bitbucket.bitbucket.*;
import com.atlassian.jira.plugins.bitbucket.mapper.BitbucketMapper;
import com.atlassian.jira.plugins.bitbucket.mapper.Encryptor;
import com.atlassian.sal.api.transaction.TransactionCallback;
import org.apache.commons.lang.StringUtils;

import java.util.*;

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

    public List<RepositoryUri> getRepositories(final String projectKey)
    {
        return activeObjects.executeInTransaction(new TransactionCallback<List<RepositoryUri>>()
        {
            public List<RepositoryUri> doInTransaction()
            {
                ProjectMapping[] mappings = activeObjects.find(
                        ProjectMapping.class, "project_key = ?", projectKey);
                List<RepositoryUri> result = new ArrayList<RepositoryUri>();
                for (ProjectMapping mapping : mappings)
                    result.add(RepositoryUri.parse(mapping.getRepositoryUri()));
                return result;
            }
        });
    }

    public void addRepository(String projectKey, RepositoryUri repositoryUri, String username, String password)
    {
        // TODO don't create duplicate mapping
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("repository_uri", repositoryUri.getRepositoryUri());
        map.put("project_key", projectKey);
        if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password))
        {
            map.put("username", username);
            map.put("password", encryptor.encrypt(password, projectKey, repositoryUri.getRepositoryUrl()));
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

    public void removeRepository(final String projectKey, final RepositoryUri repositoryUri)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Object>()
        {
            public Object doInTransaction()
            {
                activeObjects.delete(activeObjects.find(ProjectMapping.class,
                        "project_key = ? and repository_uri = ?",
                        projectKey, repositoryUri.getRepositoryUri()));

                activeObjects.delete(activeObjects.find(IssueMapping.class,
                        "project_key = ? and repository_uri = ?",
                        projectKey, repositoryUri.getRepositoryUri()));
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

                IssueMapping[] mappings = activeObjects.find(IssueMapping.class, "issue_id = ?", issueId);
                List<BitbucketChangeset> changesets = new ArrayList<BitbucketChangeset>();
                for (IssueMapping mapping : mappings)
                {
                    RepositoryUri repositoryUri = RepositoryUri.parse(mapping.getRepositoryUri());
                    BitbucketAuthentication auth = getAuthentication(getProjectMapping(projectKey, repositoryUri));
                    changesets.add(bitbucket.getChangeset(auth, repositoryUri.getOwner(), repositoryUri.getSlug(), mapping.getNode()));
                }

                // BBC-57
                Collections.sort(changesets, new Comparator<BitbucketChangeset>()
                {
                    public int compare(BitbucketChangeset a, BitbucketChangeset b)
                    {
                        return a.getRevision() - b.getRevision();
                    }
                });

                return changesets;
            }
        });
    }

    public void addChangeset(final String issueId, final BitbucketChangeset changeset)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Object>()
        {
            public Object doInTransaction()
            {
                final Map<String, Object> map = new HashMap<String, Object>();
                map.put("node", changeset.getNode());
                map.put("project_key", getProjectKey(issueId));
                map.put("issue_id", issueId);
                map.put("repository_uri", new RepositoryUri(changeset.getRepositoryOwner(), changeset.getRepositorySlug()).toString());
                IssueMapping[] mappings = activeObjects.find(IssueMapping.class,
                        "issue_id = ? and node = ?",
                        issueId, changeset.getNode());
                if (mappings != null && mappings.length > 0)
                    activeObjects.delete(mappings);
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

    public BitbucketAuthentication getAuthentication(String projectKey, RepositoryUri repositoryUri)
    {
        return getAuthentication(getProjectMapping(projectKey, repositoryUri));
    }

    private ProjectMapping getProjectMapping(String projectKey, RepositoryUri repositoryUri)
    {
        ProjectMapping[] projectMappings = activeObjects.find(ProjectMapping.class,
                "project_key = ? and repository_uri = ?",
                projectKey, repositoryUri.getRepositoryUri());
        if (projectMappings == null || projectMappings.length != 1)
            throw new BitbucketException("invalid mapping for project [ " + projectKey + " ] to " +
                    "repository [ " + repositoryUri.getRepositoryUri() + " ] was [ " +
                    (projectMappings == null ? "null" : String.valueOf(projectMappings.length)) + " ]");
        return projectMappings[0];
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
