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
    private static final String DETAIL_URL_SUFFIX = "detail";
    private final EntityLocalRestpoint<RestDevResponseForRestChangesetRepository> entityLocalRestpoint = new EntityLocalRestpoint(RestDevResponseForRestChangesetRepository.class, DETAIL_URL_SUFFIX);

    /**
     * Hack for generic de-serialization, taken from {@link com.atlassian.jira.plugins.dvcs.pageobjects.remoterestpoint.PullRequestLocalRestpoint}
     */
    private static class RestDevResponseForRestChangesetRepository extends RestDevResponse<RestChangesetRepository>
    {
    }

    /**
     * Calls {@link com.atlassian.jira.plugins.dvcs.pageobjects.remoterestpoint.EntityLocalRestpoint#getEntity(String,
     * com.google.common.base.Function)} with {@link com.atlassian.jira.plugins.dvcs.pageobjects.remoterestpoint.ChangesetLocalRestpoint.ChangesetPredicate}
     * and flattens the result into a List of commit messages, this does involve retrying the fetch if it is not found at first
     *
     * @param issueKey The issue key to search for
     * @param expectedNumberOfChangesets The number of changesets we expect to find, the fetch will retry until we get this many
     * @return The commit messages across all changesets we find
     */
    public List<String> getCommitMessages(String issueKey, int expectedNumberOfChangesets)
    {
        return convertToCommitMessages(entityLocalRestpoint.getEntity(issueKey, new ChangesetPredicate(expectedNumberOfChangesets)));
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
