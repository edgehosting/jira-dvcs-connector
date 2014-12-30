package com.atlassian.jira.plugins.dvcs.pageobjects.remoterestpoint;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.plugins.dvcs.model.dev.RestDevResponse;
import com.atlassian.jira.plugins.dvcs.model.dev.RestPrRepository;
import com.google.common.base.Function;

import javax.annotation.Nullable;

/**
 * @author Miroslav Stencel <mstencel@atlassian.com>
 */
public class PullRequestLocalRestpoint
{
    private static final String DETAIL_URL_SUFFIX = "pr-detail";
    private final EntityLocalRestpoint<RestDevResponseForPrRepository> entityLocalRestpoint = new EntityLocalRestpoint(RestDevResponseForPrRepository.class, DETAIL_URL_SUFFIX);

    /**
     * Hack for generic de-serialization.
     *
     * @author Stanislav Dvorscak
     */
    private static class RestDevResponseForPrRepository extends RestDevResponse<RestPrRepository>
    {
    }

    /**
     * Calls {@link com.atlassian.jira.plugins.dvcs.pageobjects.remoterestpoint.EntityLocalRestpoint#getEntity(String,
     * com.google.common.base.Function)} with {@link com.atlassian.jira.plugins.dvcs.pageobjects.remoterestpoint.PullRequestLocalRestpoint.SingleRestPrRepositoryPredicate}
     * which returns the {@link com.atlassian.jira.plugins.dvcs.model.dev.RestPrRepository} that are found for the issue
     * key, this does involve retrying the fetch if it is not found at first
     *
     * @param issueKey The issue key to search for
     * @return The pull request(s) that were found
     */
    public RestDevResponse<RestPrRepository> getAtLeastOnePullRequest(String issueKey)
    {
        return entityLocalRestpoint.getEntity(issueKey, new SingleRestPrRepositoryPredicate());
    }

    /**
     * Calls {@link com.atlassian.jira.plugins.dvcs.pageobjects.remoterestpoint.EntityLocalRestpoint#getEntity(String,
     * com.google.common.base.Function)} with {@link com.atlassian.jira.plugins.dvcs.pageobjects.remoterestpoint.PullRequestLocalRestpoint.SingleRestPrRepositoryPredicate}
     * which returns the {@link com.atlassian.jira.plugins.dvcs.model.dev.RestPrRepository} that are found for the issue
     * key, this does involve retrying the fetch if it is not found at first
     *
     * @param issueKey The issue key to search for
     * @param jira The @{link JiraTestedProduct} to use instead
     * @return The pull request(s) that were found
     */
    public RestDevResponse<RestPrRepository> getAtLeastOnePullRequest(String issueKey, JiraTestedProduct jira)
    {
        return entityLocalRestpoint.getEntity(issueKey, new SingleRestPrRepositoryPredicate(), jira);
    }

    private static class SingleRestPrRepositoryPredicate implements Function<RestDevResponseForPrRepository, Boolean>
    {
        @Override
        public Boolean apply(@Nullable final RestDevResponseForPrRepository input)
        {
            return !input.getRepositories().isEmpty();
        }
    }
}
