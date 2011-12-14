package com.atlassian.jira.plugins.bitbucket.spi.github.impl;

import com.atlassian.jira.plugins.bitbucket.IssueLinker;
import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.Encryptor;
import com.atlassian.jira.plugins.bitbucket.api.RepositoryPersister;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlException;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.spi.Communicator;
import com.atlassian.jira.plugins.bitbucket.spi.DvcsRepositoryManager;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryUri;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.templaterenderer.TemplateRenderer;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class GithubRepositoryManager extends DvcsRepositoryManager
{
    private static final Logger LOG = LoggerFactory.getLogger(GithubRepositoryManager.class);

    public static final String GITHUB = "github";

    public GithubRepositoryManager(RepositoryPersister repositoryPersister, @Qualifier("githubCommunicator") Communicator communicator,
                                   Encryptor encryptor, ApplicationProperties applicationProperties, IssueLinker issueLinker, TemplateRenderer templateRenderer)
    {
        super(communicator, repositoryPersister, encryptor, applicationProperties, issueLinker, templateRenderer);
    }

    @Override
    public List<Changeset> parsePayload(SourceControlRepository repository, String payload)
    {
        LOG.debug("parsing payload: '{}' for repository [{}]", payload, repository);
        List<Changeset> changesets = new ArrayList<Changeset>();
        try
        {
            JSONObject jsonPayload = new JSONObject(payload);
            JSONArray commits = jsonPayload.getJSONArray("commits");

            for (int i = 0; i < commits.length(); ++i)
            {
                // from post commit service we haven't all data that we need. we have to make next request for it.
                JSONObject commitJson = commits.getJSONObject(i);
                String commitId = commitJson.getString("id");
                Changeset changeset = getCommunicator().getChangeset(repository, commitId);
                changesets.add(changeset);
            }
        } catch (JSONException e)
        {
            throw new SourceControlException(e);
        }
        return changesets;
    }

    @Override
    public String getRepositoryType()
    {
        return GITHUB;
    }

    @Override
    public RepositoryUri getRepositoryUri(String urlString)
    {
        try
        {
            URL url = new URL(urlString);
            String protocol = url.getProtocol();
            String hostname = url.getHost();
            String path = url.getPath();
            String[] split = StringUtils.split(path, "/");
            if (split.length < 2)
            {
                throw new SourceControlException("Expected url is https://domainname.com/username/repository");
            }
            String owner = split[0];
            String slug = split[1];
            return new GithubRepositoryUri(protocol, hostname, owner, slug);
        } catch (MalformedURLException e)
        {
            throw new SourceControlException("Invalid url [" + urlString + "]");
        }
    }
}
