package com.atlassian.jira.plugins.dvcs.spi.bitbucket.message;

import com.atlassian.jira.plugins.dvcs.service.message.AbstractMessagePayloadSerializer;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONObject;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Component
public class BitbucketSynchronizeActivityMessageSerializer
        extends AbstractMessagePayloadSerializer<BitbucketSynchronizeActivityMessage>
{

    @Override
    protected void serializeInternal(JSONObject json, BitbucketSynchronizeActivityMessage payload) throws Exception
    {
        json.put("page", payload.getPageNum());
        json.put("processedPullRequests", payload.getProcessedPullRequests());
        json.put("processedPullRequestsLocal", payload.getProcessedPullRequestsLocal());
        if (payload.getLastSyncDate() != null)
        {
            json.put("lastSyncDate", payload.getLastSyncDate().getTime());
        }
    }

    @Override
    protected BitbucketSynchronizeActivityMessage deserializeInternal(JSONObject json, final int version)
            throws Exception
    {
        Set<Integer> processedPullRequests;
        Set<Integer> processedPullRequestsLocal;
        Date lastSyncDate = null;
        int page = 1;

        page = json.optInt("page");
        processedPullRequests = asSet(json.optJSONArray("processedPullRequests"));
        processedPullRequestsLocal = asSet(json.optJSONArray("processedPullRequestsLocal"));
        lastSyncDate = parseDate(json, "lastSyncDate", version);

        return new BitbucketSynchronizeActivityMessage(null, null, false, page, processedPullRequests, processedPullRequestsLocal, lastSyncDate, 0, false);
    }


    private Set<Integer> asSet(JSONArray optJSONArray)
    {
        Set<Integer> ret = new HashSet<Integer>();
        if (optJSONArray == null)
        {
            return ret;
        }
        for (int i = 0; i < optJSONArray.length(); i++)
        {
            ret.add(optJSONArray.optInt(i));
        }
        return ret;
    }


    @Override
    public Class<BitbucketSynchronizeActivityMessage> getPayloadType()
    {
        return BitbucketSynchronizeActivityMessage.class;
    }

}
