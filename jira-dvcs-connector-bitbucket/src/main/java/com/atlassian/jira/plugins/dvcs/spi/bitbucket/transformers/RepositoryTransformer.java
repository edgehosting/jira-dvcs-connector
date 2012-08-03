package com.atlassian.jira.plugins.dvcs.spi.bitbucket.transformers;


import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketRepository;


/**
 * RepositoryTransformer
 *
 * @author Martin Skurla mskurla@atlassian.com
 */
public final class RepositoryTransformer {
    private RepositoryTransformer() {}


    @SuppressWarnings("unchecked")
    public static List<Repository> fromBitbucketRepositories(List<BitbucketRepository> bitbucketRepositories)
    {
        return (List<Repository>) CollectionUtils.collect(bitbucketRepositories, new Transformer() {

            @Override
            public Object transform(Object input)
            {
                BitbucketRepository bitbucketRepository = (BitbucketRepository) input;

                Repository repository = new Repository();

                repository.setName(bitbucketRepository.getName());
                repository.setSlug(bitbucketRepository.getSlug());

                return repository;
            }
        });
    }
}
