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
    public static final String GITHUB_URL = "https://github.com";
    public static final String GITHUB_API_URL = "https://api.github.com";

    public static final String REPOSITORY_NAME = "test-project";

    public static List<String> getHookUrls(final String accountName, final String gitUrl, final String project)
    {
        final GitHubRESTClientImpl restClient = new GitHubRESTClientImpl();
        final Repository repository = new Repository();
        repository.setOrgHostUrl(gitUrl);
        repository.setOrgName(accountName);
        repository.setSlug(project);
        repository.setCredential(new Credential());

        List<GitHubRepositoryHook> hooks = restClient.getHooks(repository, accountName, PasswordUtil.getPassword(accountName));

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
