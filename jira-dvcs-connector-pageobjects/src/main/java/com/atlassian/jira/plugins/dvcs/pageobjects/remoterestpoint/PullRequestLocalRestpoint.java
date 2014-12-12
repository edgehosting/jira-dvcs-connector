package com.atlassian.jira.plugins.dvcs.pageobjects.remoterestpoint;

import com.atlassian.jira.plugins.dvcs.model.dev.RestDevResponse;
import com.atlassian.jira.plugins.dvcs.model.dev.RestPrRepository;
import com.google.common.base.Function;

import javax.annotation.Nullable;

/**
 * @author Miroslav Stencel <mstencel@atlassian.com>
 */
public class PullRequestLocalRestpoint
{
    public static final String DETAIL_URL_SUFFIX = "pr-detail";
    private final EntityLocalRestpoint<RestDevResponseForPrRepository> entityLocalRestpoint = new EntityLocalRestpoint(RestDevResponseForPrRepository.class, DETAIL_URL_SUFFIX);

    /**
     * Hack for generic de-serialization.
     *
     * @author Stanislav Dvorscak
     */
    public static class RestDevResponseForPrRepository extends RestDevResponse<RestPrRepository>
    {
    }

    /**
     * REST point for "/rest/bitbucket/1.0/jira-dev/pr-detail?issue=" + issueKey
     *
     * @return RestDevResponse<RestPrRepository>
     */
    public RestDevResponse<RestPrRepository> getPullRequest(String issueKey)
    {
        return entityLocalRestpoint.getEntity(issueKey);
    }

    public RestDevResponse<RestPrRepository> retryingGetAtLeastOnePullRequest(String issueKey)
    {
        return entityLocalRestpoint.retryingGetEntity(issueKey, new SingleRestPrRepositoryPredicate());
    }

    private static class SingleRestPrRepositoryPredicate implements Function<RestDevResponseForPrRepository, Boolean>
    {
        @Override
        public Boolean apply(@Nullable final RestDevResponseForPrRepository input)
        {
            return input.getRepositories().size() > 0;
        }
    }
}
