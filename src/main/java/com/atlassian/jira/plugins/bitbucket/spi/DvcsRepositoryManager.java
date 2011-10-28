package com.atlassian.jira.plugins.bitbucket.spi;

import com.atlassian.jira.plugins.bitbucket.activeobjects.v2.IssueMapping;
import com.atlassian.jira.plugins.bitbucket.activeobjects.v2.ProjectMapping;
import com.atlassian.jira.plugins.bitbucket.api.*;
import com.atlassian.jira.plugins.bitbucket.api.impl.DefaultSourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.RepositoryUri;
import com.atlassian.sal.api.ApplicationProperties;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

public abstract class DvcsRepositoryManager implements RepositoryManager {

    private final Encryptor encryptor;
    private final ApplicationProperties applicationProperties;

	/* Maps ProjectMapping to SourceControlRepository */
	private final Function<ProjectMapping, SourceControlRepository> TO_SOURCE_CONTROL_REPOSITORY =
			new Function<ProjectMapping, SourceControlRepository>()
			{
				public SourceControlRepository apply(ProjectMapping pm)
				{
					String decryptedPassword = encryptor.decrypt(pm.getPassword(), pm.getProjectKey(), pm.getRepositoryUrl());
					return new DefaultSourceControlRepository(pm.getID(), RepositoryUri.parse(pm.getRepositoryUrl()).getRepositoryUrl(),
							pm.getProjectKey(), pm.getUsername(), decryptedPassword);
				}
			};

	private final Function<IssueMapping, Changeset> TO_CHANGESET =
			new Function<IssueMapping, Changeset>()
			{
				public Changeset apply(IssueMapping from)
				{
					ProjectMapping pm = getRepositoryPersister().getRepository(from.getRepositoryId());
                    SourceControlRepository repository = TO_SOURCE_CONTROL_REPOSITORY.apply(pm);
					return getCommunicator().getChangeset(repository, from.getNode());
				}
			};


    public DvcsRepositoryManager(Encryptor encryptor, ApplicationProperties applicationProperties) {
        this.encryptor = encryptor;
        this.applicationProperties = applicationProperties;
    }

    public SourceControlRepository addRepository(String projectKey, String repositoryUrl, String username, String password) {
        // Remove trailing slashes from URL
        if (repositoryUrl.endsWith("/")) {
            repositoryUrl = repositoryUrl.substring(0, repositoryUrl.length() - 1);
        }

        // Set all URLs to HTTPS
        if (repositoryUrl.startsWith("http:")) {
            repositoryUrl = repositoryUrl.replaceFirst("http:", "https:");
        }

        String encryptedPassword = encryptor.encrypt(password, projectKey, repositoryUrl);
        ProjectMapping pm = getRepositoryPersister().addRepository(projectKey, repositoryUrl, username, encryptedPassword, getRepositoryTypeId());
        return TO_SOURCE_CONTROL_REPOSITORY.apply(pm);
    }

    public SourceControlRepository getRepository(int repositoryId) {
        ProjectMapping repository = getRepositoryPersister().getRepository(repositoryId);
        return TO_SOURCE_CONTROL_REPOSITORY.apply(repository);
    }

    public List<SourceControlRepository> getRepositories(String projectKey) {
        List<ProjectMapping> repositories = getRepositoryPersister().getRepositories(projectKey, getRepositoryTypeId());
        return Lists.transform(repositories, TO_SOURCE_CONTROL_REPOSITORY);
    }

    public List<Changeset> getChangesets(String issueKey) {
        List<IssueMapping> issueMappings = getRepositoryPersister().getIssueMappings(issueKey);
        return Lists.transform(issueMappings, TO_CHANGESET);
    }

    public void removeRepository(int id) {
        getRepositoryPersister().removeRepository(id);
        // TODO Should we also delete IssueMappings? Yes we should.
    }

    public void addChangeset(SourceControlRepository repository, String issueId, Changeset changeset) {
        getRepositoryPersister().addChangeset(issueId, changeset.getRepositoryId(), changeset.getNode());
    }

    public SourceControlUser getUser(SourceControlRepository repository, String username) {
        return getCommunicator().getUser(repository, username);
    }

    public abstract String getRepositoryTypeId();
    public abstract RepositoryPersister getRepositoryPersister();
    public abstract Communicator getCommunicator();

    public abstract boolean canHandleUrl(String url);
    public abstract SynchronisationOperation getSynchronisationOperation(SynchronizationKey key, ProgressWriter progress);
    public abstract List<Changeset> parsePayload(SourceControlRepository repository, String payload);
    public abstract String getHtmlForChangeset(SourceControlRepository repository, Changeset changeset);

    protected String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("required encoding not found");
        }
    }

    public ApplicationProperties getApplicationProperties() {
        return applicationProperties;
    }




}
