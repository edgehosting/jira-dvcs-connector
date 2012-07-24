package com.atlassian.jira.plugins.dvcs.spi.bitbucket.transformers;


import java.util.List;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketRepository;
import com.google.common.base.Function;
import com.google.common.collect.Lists;


/**
 * RepositoryTransformer
 *
 * @author Martin Skurla mskurla@atlassian.com
 */
public final class RepositoryTransformer {
    private RepositoryTransformer() {}

    
    public static List<Repository> fromBitbucketRepositories(List<BitbucketRepository> bitbucketRepositories)
    {
        return Lists.transform(bitbucketRepositories, new Function<BitbucketRepository, Repository>() {

            @Override
            public Repository apply(BitbucketRepository bitbucketRepository)
            {
                Repository repository = new Repository();
                
                repository.setName(bitbucketRepository.getName());
                repository.setSlug(bitbucketRepository.getSlug());
                
                return repository;
            }
        });
    }
}
