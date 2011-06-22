package com.atlassian.jira.plugins.bitbucket.bitbucket;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.bitbucket.bitbucket.activeobjects.BitbucketRepositoryProjectMapping;
import com.atlassian.jira.plugins.bitbucket.bitbucket.remote.LazyLoadedRemoteBitbucketRepository;
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

    public DefaultBitbucketMapper(ActiveObjects activeObjects, Bitbucket bitbucket)
    {
        this.activeObjects = activeObjects;
        this.bitbucket = bitbucket;
    }

    public List<BitbucketRepository> getRepositories(final String projectKey)
    {
        return activeObjects.executeInTransaction(new TransactionCallback<List<BitbucketRepository>>()
        {
            public List<BitbucketRepository> doInTransaction()
            {
                BitbucketRepositoryProjectMapping[] mappings = activeObjects.find(BitbucketRepositoryProjectMapping.class, "projectKey = ?", projectKey);
                List<BitbucketRepository> repositories = new ArrayList<BitbucketRepository>();
                for (BitbucketRepositoryProjectMapping mapping : mappings)
                {
                    BitbucketAuthentication auth = BitbucketAuthentication.ANONYMOUS;
                    if (StringUtils.isNotBlank(mapping.getUsername()) && StringUtils.isNotBlank(mapping.getPassword()))
                        BitbucketAuthentication.basic(mapping.getUsername(), mapping.getPassword());
                    repositories.add(new LazyLoadedRemoteBitbucketRepository(bitbucket, auth, mapping.getRepositoryOwner(), mapping.getRepositorySlug()));
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
            map.put("password", password);
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

    public void removeRepository(String projectKey, BitbucketRepository repository)
    {
    }
}
