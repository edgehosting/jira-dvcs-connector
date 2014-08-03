package com.atlassian.jira.plugins.dvcs.spi.github;

import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.client.PagedRequest;
import org.eclipse.egit.github.core.service.PullRequestService;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom PullRequestService
 * We need to rewrite some methods to support more functionality from GitHub REST API
 *
 * @author Miroslav Stencel <mstencel@atlassian.com>
 *
 */
public class CustomPullRequestService extends PullRequestService
{
    public static final String DIRECTION = "direction";

    public static final String DIRECTION_ASC = "asc";
    public static final String DIRECTION_DESC = "desc";

    public static final String SORT = "sort";

    public static final String SORT_CREATED = "created";
    public static final String SORT_UPDATED = "updated";
    public static final String SORT_POPULARITY ="popularity";
    public static final String SORT_LONG_RUNNING = "long-running";

    public static final String STATE_ALL = "all";

    public CustomPullRequestService()
    {
        super();
    }

    public CustomPullRequestService(final GitHubClient client)
    {
        super(client);
    }

    public PageIterator<PullRequest> pagePullRequests(final IRepositoryIdProvider repository, final String state, final String sort, final String direction, final int start, final int size)
    {
        PagedRequest<PullRequest> request = createPullsRequest(repository, state, sort, direction, start, size);
        return createPageIterator(request);
    }

    protected PagedRequest<PullRequest> createPullsRequest(final IRepositoryIdProvider provider, final String state, final String sort, final String direction, final int start, final int size)
    {
        PagedRequest<PullRequest> request = createPullsRequest(provider, state, start, size);
        if (sort != null && direction != null)
        {
            Map<String, String> params = new HashMap<String, String>(request.getParams());
            if (sort != null)
            {
                params.put(SORT, sort);
            }

            if (direction != null)
            {
                params.put(DIRECTION, direction);
            }

            request.setParams(params);
        }

        return request;
    }

    public enum PullRequestSortType
    {
        CREATED("created"),
        UPDATED("updated"),
        POPULARITY("popularity"),
        LONG_RUNNING("long-running");

        private final String parameterValue;

        PullRequestSortType(String parameterValue)
        {
            this.parameterValue = parameterValue;
        }

        public String getParameterValue()
        {
            return parameterValue;
        }
    }
}
