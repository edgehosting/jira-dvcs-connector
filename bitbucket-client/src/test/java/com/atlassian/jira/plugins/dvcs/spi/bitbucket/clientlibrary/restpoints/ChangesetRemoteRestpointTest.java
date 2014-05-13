package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangesetPage;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteRequestor;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.ResponseCallback;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * TODO: Document this class / interface here
 *
 * @since v1.4.14
 */
public class ChangesetRemoteRestpointTest
{
    @Mock
    RemoteRequestor remoteRequestor;

    @Mock
    ResponseCallback<BitbucketChangesetPage> bitbucketChangesetPageResponseCallback;


    @BeforeClass
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetNextChangesetsPageOnFirstPageHappyPath() throws Exception
    {
        ChangesetRemoteRestpoint changesetRemoteRestpoint = new ChangesetRemoteRestpoint(remoteRequestor,bitbucketChangesetPageResponseCallback);

        //Given
        final String orgName = "org";
        final String slug = "slug";
        List<String> includeNodes = new ArrayList<String>();
        includeNodes.add("included");
        List<String> excludeNodes = new ArrayList<String>();
        excludeNodes.add("excluded");
        Map<String,List<String>> parameters = new HashMap<String, List<String>>();
        parameters.put("include",includeNodes);
        parameters.put("exclude", excludeNodes);
        final int changesetLimit = 2;
        BitbucketChangesetPage currentPage = null;
        BitbucketChangesetPage expected_result = new BitbucketChangesetPage();

        //when
        when(remoteRequestor.post(eq("/api/2.0/repositories/" + orgName + "/" + slug + "/commits/?pagelen="+Integer.toString(changesetLimit)+"&page=1"),

            any(Map.class),eq(bitbucketChangesetPageResponseCallback))).thenReturn(expected_result);


        //expect
        Assert.assertEquals(changesetRemoteRestpoint.getNextChangesetsPage(orgName, slug, includeNodes, excludeNodes, changesetLimit, currentPage), expected_result, "didn't return expected changeset page because requestor got wrong parameters");

    }
}
