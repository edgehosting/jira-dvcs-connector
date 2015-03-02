package com.atlassian.jira.plugins.dvcs.dao.impl.transform;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.OrganizationMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryMapping;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.DefaultProgress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class RepositoryTransformer
{
    private static final Logger log = LoggerFactory.getLogger(RepositoryTransformer.class);

    public Repository transform(RepositoryMapping repositoryMapping, OrganizationMapping organizationMapping, DefaultProgress defaultProgress)
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
            if (organizationMapping != null)
            {
                forkOfRepository.setRepositoryUrl(createForkOfRepositoryUrl(repositoryMapping, organizationMapping));
            }
            repository.setForkOf(forkOfRepository);
        }

        if (organizationMapping != null)
        {
            Credential credential = new Credential(organizationMapping.getOauthKey(), organizationMapping.getOauthSecret(),
                    organizationMapping.getAccessToken(), organizationMapping.getAdminUsername(), organizationMapping.getAdminPassword());
            repository.setCredential(credential);
            repository.setDvcsType(organizationMapping.getDvcsType());
            repository.setOrgHostUrl(organizationMapping.getHostUrl());
            repository.setOrgName(organizationMapping.getName());
            repository.setRepositoryUrl(createRepositoryUrl(repositoryMapping, organizationMapping));
        }
        else
        {
            repository.setOrgHostUrl(null);
            repository.setOrgName(null);
            repository.setRepositoryUrl(null);
        }

        return repository;
    }


    private String createRepositoryUrl(RepositoryMapping repositoryMapping, OrganizationMapping organizationMapping)
    {
        return createRepositoryUrl(organizationMapping.getHostUrl(), organizationMapping.getName(), repositoryMapping.getSlug());
    }

    private String createForkOfRepositoryUrl(RepositoryMapping repositoryMapping, OrganizationMapping organizationMapping)
    {
        return createRepositoryUrl(organizationMapping.getHostUrl(), repositoryMapping.getForkOfOwner(), repositoryMapping.getForkOfSlug());
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
