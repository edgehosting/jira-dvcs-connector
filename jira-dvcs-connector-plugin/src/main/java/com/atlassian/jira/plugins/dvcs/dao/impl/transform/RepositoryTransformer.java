package com.atlassian.jira.plugins.dvcs.dao.impl.transform;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryMapping;
import com.atlassian.jira.plugins.dvcs.model.DefaultProgress;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class RepositoryTransformer
{
    private static final Logger log = LoggerFactory.getLogger(RepositoryTransformer.class);

    public Repository transform(RepositoryMapping repositoryMapping, Organization organization, DefaultProgress defaultProgress)
    {
        if (repositoryMapping == null)
        {
            return null;
        }

        log.debug("Repository transformation: [{}] ", repositoryMapping);

        Repository repository = new Repository(repositoryMapping.getID(), repositoryMapping.getOrganizationId(), null,
                repositoryMapping.getSlug(), repositoryMapping.getName(), repositoryMapping.getLastCommitDate(),
                repositoryMapping.isLinked(), repositoryMapping.isDeleted(), null);
        repository.setSmartcommitsEnabled(repositoryMapping.isSmartcommitsEnabled());
        repository.setActivityLastSync(repositoryMapping.getActivityLastSync());

        Date lastDate = repositoryMapping.getLastCommitDate();

        if (lastDate == null || (repositoryMapping.getActivityLastSync() != null && repositoryMapping.getActivityLastSync().after(lastDate)))
        {
            lastDate = repositoryMapping.getActivityLastSync();
        }
        repository.setLastActivityDate(lastDate);
        repository.setLogo(repositoryMapping.getLogo());
        // set sync progress
        repository.setSync(defaultProgress);
        repository.setFork(repositoryMapping.isFork());

        if (repository.isFork() && repositoryMapping.getForkOfSlug() != null)
        {
            Repository forkOfRepository = new Repository();
            forkOfRepository.setSlug(repositoryMapping.getForkOfSlug());
            forkOfRepository.setName(repositoryMapping.getForkOfName());
            forkOfRepository.setOwner(repositoryMapping.getForkOfOwner());
            if (organization != null)
            {
                forkOfRepository.setRepositoryUrl(createForkOfRepositoryUrl(repositoryMapping, organization));
            }
            repository.setForkOf(forkOfRepository);
        }

        if (organization != null)
        {
            repository.setCredential(organization.getCredential());
            repository.setDvcsType(organization.getDvcsType());
            repository.setOrgHostUrl(organization.getHostUrl());
            repository.setOrgName(organization.getName());
            repository.setRepositoryUrl(createRepositoryUrl(repositoryMapping, organization));
        }
        else
        {
            repository.setOrgHostUrl(null);
            repository.setOrgName(null);
            repository.setRepositoryUrl(null);
        }

        return repository;
    }

    private String createRepositoryUrl(RepositoryMapping repositoryMapping, Organization organization)
    {
        return createRepositoryUrl(organization.getHostUrl(), organization.getName(), repositoryMapping.getSlug());
    }

    private String createForkOfRepositoryUrl(RepositoryMapping repositoryMapping, Organization organization)
    {
        return createRepositoryUrl(organization.getHostUrl(), repositoryMapping.getForkOfOwner(), repositoryMapping.getForkOfSlug());
    }

    public static String createRepositoryUrl(String hostUrl, String owner, String slug)
    {
        // normalize
        if (hostUrl != null && hostUrl.endsWith("/"))
        {
            hostUrl = hostUrl.substring(0, hostUrl.length() - 1);
        }
        return hostUrl + "/" + owner + "/" + slug;
    }
}
