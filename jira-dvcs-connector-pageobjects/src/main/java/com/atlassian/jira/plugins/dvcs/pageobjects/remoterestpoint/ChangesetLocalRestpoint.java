package com.atlassian.jira.plugins.dvcs.pageobjects.remoterestpoint;

import com.atlassian.fugue.Iterables;
import com.atlassian.jira.plugins.dvcs.model.dev.RestChangeset;
import com.atlassian.jira.plugins.dvcs.model.dev.RestChangesetRepository;
import com.atlassian.jira.plugins.dvcs.model.dev.RestDevResponse;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

import java.util.List;
import javax.annotation.Nullable;

/**
 * Fetches changesets from the dev detail endpoint
 */
public class ChangesetLocalRestpoint
{
    public static final String DETAIL_URL_SUFFIX = "detail";
    private final EntityLocalRestpoint<RestDevResponseForRestChangesetRepository> entityLocalRestpoint = new EntityLocalRestpoint(RestDevResponseForRestChangesetRepository.class, DETAIL_URL_SUFFIX);

    /**
     * Hack for generic de-serialization, taken from {@link com.atlassian.jira.plugins.dvcs.pageobjects.remoterestpoint.PullRequestLocalRestpoint}
     */
    public static class RestDevResponseForRestChangesetRepository extends RestDevResponse<RestChangesetRepository>
    {
    }

    public List<String> retryingGetCommitMessages(String issueKey, int expectedNumberOfChangesets)
    {
        return convertToCommitMessages(entityLocalRestpoint.retryingGetEntity(issueKey, new ChangesetPredicate(expectedNumberOfChangesets)));
    }

    private static List<String> convertToCommitMessages(RestDevResponse<RestChangesetRepository> response)
    {
        List<RestChangesetRepository> results = response.getRepositories();
        Iterable<String> transformedResults = Iterables.flatMap(results, new Function<RestChangesetRepository, Iterable<String>>()
        {
            @Override
            public Iterable<String> apply(@Nullable final RestChangesetRepository input)
            {
                return Collections2.transform(input.getCommits(), new Function<RestChangeset, String>()
                {
                    @Override
                    public String apply(@Nullable final RestChangeset input)
                    {
                        return input.getMessage();
                    }
                });
            }
        });

        return Lists.newArrayList(transformedResults);
    }

    private static class ChangesetPredicate implements Function<RestDevResponseForRestChangesetRepository, Boolean>
    {
        private final int expectedNumberOfChangesets;

        private ChangesetPredicate(final int expectedNumberOfChangesets)
        {
            this.expectedNumberOfChangesets = expectedNumberOfChangesets;
        }

        @Override
        public Boolean apply(@Nullable final RestDevResponseForRestChangesetRepository input)
        {
            List<String> messages = convertToCommitMessages(input);
            return messages.size() == expectedNumberOfChangesets;
        }
    }
}
