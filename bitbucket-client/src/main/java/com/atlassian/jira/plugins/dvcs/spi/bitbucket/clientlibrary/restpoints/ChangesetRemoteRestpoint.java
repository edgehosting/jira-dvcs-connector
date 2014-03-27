package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.ClientUtils;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangeset;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangesetPage;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangesetWithDiffstat;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketNewChangeset;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BitbucketRequestException;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteRequestor;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteResponse;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.ResponseCallback;
import com.atlassian.jira.util.UrlBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang.StringUtils;

/**
 * ChangesetRemoteRestpoint
 *
 * @author Martin Skurla mskurla@atlassian.com
 */
public class ChangesetRemoteRestpoint
{
    private final RemoteRequestor requestor;
    private final ResponseCallback<BitbucketChangesetPage> bitbucketChangesetPageResponseCallback;

    public ChangesetRemoteRestpoint(RemoteRequestor remoteRequestor, ResponseCallback<BitbucketChangesetPage> bitbucketChangesetPageResponseCallback)
    {
        this.requestor = remoteRequestor;
        this.bitbucketChangesetPageResponseCallback = bitbucketChangesetPageResponseCallback;
    }

    public BitbucketChangeset getChangeset(String owner, String slug, String node)
    {
        String getChangesetUrl = URLPathFormatter.format("/repositories/%s/%s/changesets/%s", owner, slug, node);

        return requestor.get(getChangesetUrl, null, new ResponseCallback<BitbucketChangeset>()
        {

            @Override
            public BitbucketChangeset onResponse(RemoteResponse response)
            {
                return ClientUtils.fromJson(response.getResponse(), BitbucketChangeset.class);
            }
        });
    }

    // "/api/1.0/repositories/erik/bitbucket/changesets/4a233e7b8596e5b17dd672f063e40f7c544c2c81"
    public BitbucketChangeset getChangeset(String urlIncludingApi)
    {
        return requestor.get(URLPathFormatter.format(urlIncludingApi), null, new ResponseCallback<BitbucketChangeset>()
        {

            @Override
            public BitbucketChangeset onResponse(RemoteResponse response)
            {
                return ClientUtils.fromJson(response.getResponse(), BitbucketChangeset.class);
            }

        });

    }

    public List<BitbucketChangesetWithDiffstat> getChangesetDiffStat(String owner, String slug, String node, int limit)
    {
        String getChangesetDiffStatUrl = URLPathFormatter.format("/repositories/%s/%s/changesets/%s/diffstat", owner, slug, node);

        Map<String, String> parameters = Collections.singletonMap("limit", "" + limit);

        return requestor.get(getChangesetDiffStatUrl, parameters,
                new ResponseCallback<List<BitbucketChangesetWithDiffstat>>()
                {
                    @Override
                    public List<BitbucketChangesetWithDiffstat> onResponse(RemoteResponse response)
                    {
                        return ClientUtils.fromJson(response.getResponse(),
                                new TypeToken<List<BitbucketChangesetWithDiffstat>>()
                                {
                                }.getType());
                    }
                });
    }

    public BitbucketChangesetPage getNextChangesetsPage(String orgName, String slug, List<String> includeNodes, List<String> excludeNodes, int changesetLimit, BitbucketChangesetPage currentPage)
    {
        if (currentPage == null || StringUtils.isBlank(currentPage.getNext()))
        {
            // this is the first request, first page
            return makeInitialRequest(orgName, slug, includeNodes, excludeNodes, changesetLimit, currentPage);
        }

        try
        {
            return requestor.getWithMultipleVals(currentPage.getNext(), null, bitbucketChangesetPageResponseCallback);
        }
        catch (BitbucketRequestException.InternalServerError_500 e)
        {

            // "next page" is no longer valid.
            // Set it to null so that we generate the url for this page as above next time.
            currentPage.setNext(null);
            throw e;
        }
    }

    private BitbucketChangesetPage makeInitialRequest(String orgName, String slug, List<String> includeNodes, List<String> excludeNodes, int changesetLimit, BitbucketChangesetPage currentPage)
    {
        // Use the page set in currentPage if available, otherwise start at the beginning
        String url = getUrlForInitialRequest(orgName, slug, changesetLimit, currentPage);

        Map<String, List<String>> parameters = getHttpParametersMap(includeNodes, excludeNodes);

        return requestor.post(url, parameters, bitbucketChangesetPageResponseCallback);
    }

    private Map<String, List<String>> getHttpParametersMap(List<String> includeNodes, List<String> excludeNodes)
    {
        Map<String, List<String>> parameters;
        parameters = new HashMap<String, List<String>>();
        if (includeNodes != null && !includeNodes.isEmpty())
        {
            parameters.put("include", new ArrayList<String>(includeNodes));
        }
        if (excludeNodes != null && !excludeNodes.isEmpty())
        {
            parameters.put("exclude", new ArrayList<String>(excludeNodes));
        }
        return parameters;
    }

    private String getUrlForInitialRequest(String orgName, String slug, int changesetLimit, BitbucketChangesetPage currentPage)
    {
        UrlBuilder urlBuilder = new UrlBuilder("/api/2.0/repositories","UTF-8",false);
        urlBuilder.addPath(orgName);
        urlBuilder.addPath(slug);
        urlBuilder.addPathUnsafe("/commits/");
        urlBuilder.addParameter("pagelen",Integer.toString(changesetLimit));
        int pageNumber = 1;
        if (currentPage != null && currentPage.getPage() > 0)
        {
            pageNumber = currentPage.getPage();
        }
        urlBuilder.addParameter("page",Integer.toString(pageNumber));
        return urlBuilder.asUrlString();
    }
}
