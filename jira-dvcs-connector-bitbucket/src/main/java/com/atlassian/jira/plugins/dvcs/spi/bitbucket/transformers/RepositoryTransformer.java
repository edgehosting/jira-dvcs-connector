package com.atlassian.jira.plugins.dvcs.spi.bitbucket.transformers;


import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketRepository;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;

import java.util.List;


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
                repository.setLogo(bitbucketRepository.getLogo());
                repository.setFork(bitbucketRepository.isFork());
                repository.setForkOf(createForkOfRepository(bitbucketRepository.getForkOf()));
                return repository;
            }
        });
    }

    private static Repository createForkOfRepository(BitbucketRepository bitbucketRepository)
    {
        if (bitbucketRepository == null)
        {
            return null;
        }

        Repository forkRepository = new Repository();
        forkRepository.setSlug(bitbucketRepository.getSlug());
        forkRepository.setName(bitbucketRepository.getName());
        forkRepository.setOwner(bitbucketRepository.getOwner());

        return forkRepository;
    }
}
