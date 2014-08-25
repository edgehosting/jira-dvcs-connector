package it.restart.com.atlassian.jira.plugins.dvcs.test;

import com.atlassian.jira.plugins.dvcs.github.api.model.GitHubRepositoryHook;
import com.atlassian.jira.plugins.dvcs.github.impl.GitHubRESTClientImpl;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.util.PasswordUtil;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * Some static helpers for the Github tests
 */
public class GithubTestHelper
{
    private static final String ACCOUNT_NAME = "jirabitbucketconnector";

    public static List<String> getHookUrls(String gitUrl, String project)
    {
        final GitHubRESTClientImpl restClient = new GitHubRESTClientImpl();
        final Repository repository = new Repository();
        repository.setOrgHostUrl(gitUrl);
        repository.setOrgName(ACCOUNT_NAME);
        repository.setSlug(project);
        final Credential credential = new Credential();
        credential.setAccessToken("bogus");
        repository.setCredential(credential);

        List<GitHubRepositoryHook> hooks = restClient.getHooks(repository, ACCOUNT_NAME, PasswordUtil.getPassword(ACCOUNT_NAME));

        return Lists.transform(hooks, new Function<GitHubRepositoryHook, String>()
        {
            @Override
            public String apply(final GitHubRepositoryHook input)
            {
                return input.getConfig().get("url");
            }
        });
    }
}
