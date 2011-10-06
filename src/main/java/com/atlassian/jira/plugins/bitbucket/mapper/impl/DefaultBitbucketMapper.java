package com.atlassian.jira.plugins.bitbucket.mapper.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.bitbucket.activeobjects.v1.IssueMapping;
import com.atlassian.jira.plugins.bitbucket.activeobjects.v1.ProjectMapping;
import com.atlassian.jira.plugins.bitbucket.bitbucket.*;
import com.atlassian.jira.plugins.bitbucket.mapper.BitbucketMapper;
import com.atlassian.jira.plugins.bitbucket.mapper.Encryptor;
import com.atlassian.sal.api.transaction.TransactionCallback;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * A simple bitbucket mapper that uses ActiveObjects to store the mapping details
 */
public class DefaultBitbucketMapper implements BitbucketMapper
{
    private final Logger logger = LoggerFactory.getLogger(DefaultBitbucketMapper.class);

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
                        ProjectMapping.class, "PROJECT_KEY = ?", projectKey);
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
        map.put("REPOSITORY_URI", repositoryUri.getRepositoryUri());
        map.put("PROJECT_KEY", projectKey);
        if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password))
        {
            map.put("USERNAME", username);
            map.put("PASSWORD", encryptor.encrypt(password, projectKey, repositoryUri.getRepositoryUrl()));
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
                final ProjectMapping[] projectMappings = activeObjects.find(ProjectMapping.class,
                        "PROJECT_KEY = ? and REPOSITORY_URI = ?",
                        projectKey, repositoryUri.getRepositoryUri());

                final IssueMapping[] issueMappings = activeObjects.find(IssueMapping.class,
                        "PROJECT_KEY = ? and REPOSITORY_URI = ?",
                        projectKey, repositoryUri.getRepositoryUri());

                logger.debug("deleting [ {} ] project mappings [ {} ] [ {} ]",
                        new String[]{String.valueOf(projectMappings.length), projectKey, repositoryUri.getRepositoryUri()});
                logger.debug("deleting [ {} ] issue mappings [ {} ] [ {} ]",
                        new String[]{String.valueOf(issueMappings.length), projectKey, repositoryUri.getRepositoryUri()});

                activeObjects.delete(projectMappings);
                activeObjects.delete(issueMappings);
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

                IssueMapping[] mappings = activeObjects.find(IssueMapping.class, "ISSUE_ID = ?", issueId);
                List<BitbucketChangeset> changesets = new ArrayList<BitbucketChangeset>();
                for (IssueMapping mapping : mappings)
                {
                    RepositoryUri repositoryUri = RepositoryUri.parse(mapping.getRepositoryUri());
                    BitbucketAuthentication auth = getAuthentication(getProjectMapping(projectKey, repositoryUri));
                    changesets.add(bitbucket.getChangeset(auth, repositoryUri.getOwner(), repositoryUri.getSlug(), mapping.getNode()));
                }
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
                final String repositoryOwner = changeset.getRepositoryOwner();
                final String repositorySlug = changeset.getRepositorySlug();

                final RepositoryUri repositoryUri = new RepositoryUri(repositoryOwner, repositorySlug);
                final String projectKey = getProjectKey(issueId);
                try
                {
                    getProjectMapping(projectKey, repositoryUri);
                    final Map<String, Object> map = new HashMap<String, Object>();
                    map.put("NODE", changeset.getNode());
                    map.put("PROJECT_KEY", projectKey);
                    map.put("ISSUE_ID", issueId);
                    map.put("REPOSITORY_URI", repositoryUri.getRepositoryUri());
                    IssueMapping[] mappings = activeObjects.find(IssueMapping.class,
                            "ISSUE_ID = ? and NODE = ?",
                            issueId, changeset.getNode());
                    logger.debug("create issue mapping [ {} ] [ {} ]", new String[]{projectKey, repositoryUri.getRepositoryUri()});
                    if (mappings != null && mappings.length > 0)
                        activeObjects.delete(mappings);
                    activeObjects.create(IssueMapping.class, map);
                }
                catch (BitbucketException e)
                {
                    // the mapping does not include the branch which this changeset was found so it is ignored
                }
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
                        "ISSUE_ID = ? and NODE = ?",
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
                "PROJECT_KEY = ? and REPOSITORY_URI = ?",
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
            auth = BitbucketAuthentication.basic(mapping.getUsername(),
                    encryptor.decrypt(mapping.getPassword(), mapping.getProjectKey(),
                            RepositoryUri.parse(mapping.getRepositoryUri()).getRepositoryUrl()));
        return auth;
    }

}
