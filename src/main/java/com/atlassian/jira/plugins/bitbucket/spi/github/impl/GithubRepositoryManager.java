package com.atlassian.jira.plugins.bitbucket.spi.github.impl;

import com.atlassian.jira.plugins.bitbucket.activeobjects.v2.ProjectMapping;
import com.atlassian.jira.plugins.bitbucket.api.*;
import com.atlassian.jira.plugins.bitbucket.api.impl.DefaultSourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.spi.Communicator;
import com.atlassian.jira.plugins.bitbucket.spi.DvcsRepositoryManager;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryManager;
import com.atlassian.jira.plugins.bitbucket.spi.SynchronisationOperation;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.RepositoryUri;
import com.atlassian.sal.api.ApplicationProperties;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GithubRepositoryManager extends DvcsRepositoryManager {

    private final RepositoryPersister repositoryPersister;
    private final Communicator githubCommunicator;

    public GithubRepositoryManager(RepositoryPersister repositoryPersister, Communicator communicator, Encryptor encryptor, ApplicationProperties applicationProperties) {
        super(encryptor, applicationProperties);

        this.repositoryPersister = repositoryPersister;
        this.githubCommunicator = communicator;
    }

    public boolean canHandleUrl(String url) {
        // Valid URL and URL starts with bitbucket.org domain
        Pattern p = Pattern.compile("^(https|http)://github.com/[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
        Matcher m = p.matcher(url);
        return m.matches();

    }

    @Override
    public RepositoryPersister getRepositoryPersister() {
        return repositoryPersister;
    }

    @Override
    public Communicator getCommunicator() {
        return githubCommunicator;
    }

    public SynchronisationOperation getSynchronisationOperation(SynchronizationKey key, ProgressWriter progress) {
        // todo
        return null;
    }

    public List<Changeset> parsePayload(SourceControlRepository repository, String payload) {
        // todo
        return new ArrayList<Changeset>();
    }

    public String getHtmlForChangeset(SourceControlRepository repository, Changeset changeset) {
        // todo
        return "<div></div>";
    }

    public String getRepositoryTypeId() {
        return "github";
    }
}
