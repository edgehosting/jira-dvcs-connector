package com.atlassian.jira.plugins.dvcs.spi.bitbucket.message.oldsync;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Nullable;

import com.atlassian.jira.plugins.dvcs.model.BranchHead;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.service.message.AbstractMessagePayloadSerializer;
import com.atlassian.jira.plugins.dvcs.service.message.MessagePayloadSerializer;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONObject;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

/**
 * An implementation of {@link MessagePayloadSerializer} over
 * {@link OldBitbucketSynchronizeCsetMsg}.
 *
 * @author Stanislav Dvorscak
 *
 */
public class OldBitbucketSynchronizeCsetMsgSerializer extends AbstractMessagePayloadSerializer<OldBitbucketSynchronizeCsetMsg>
{
    public OldBitbucketSynchronizeCsetMsgSerializer(RepositoryService repositoryService, Synchronizer synchronizer)
    {
        super(repositoryService, synchronizer);
    }

    @Override
    protected void serializeInternal(JSONObject json, OldBitbucketSynchronizeCsetMsg payload) throws Exception
    {
        json.put("branch", payload.getBranch());
        json.put("node", payload.getNode());
        json.put("refreshAfterSynchronizedAt", getDateFormat().format(payload.getRefreshAfterSynchronizedAt()));
    }

    @Override
    protected OldBitbucketSynchronizeCsetMsg deserializeInternal(JSONObject json) throws Exception
    {
        String branch;
        String node;
        Date refreshAfterSynchronizedAt;

        branch = json.getString("branch");
        node = json.getString("node");
        refreshAfterSynchronizedAt = getDateFormat().parse(json.getString("refreshAfterSynchronizedAt"));

        return new OldBitbucketSynchronizeCsetMsg(null, branch, node, refreshAfterSynchronizedAt, null, false, 0);
    }

    private List<BranchHead> toBranchHeads(JSONArray optJSONArray)
    {
        List<BranchHead> ret = new ArrayList<BranchHead>();
        if (optJSONArray == null)
        {
            return ret;
        }
        for (int i = 0; i < optJSONArray.length(); i++)
        {
            String input = optJSONArray.optString(i);
            int index = input.lastIndexOf(":");
            ret.add(new BranchHead(input.substring(0, index), input.substring(index + 1)));
        }
        return ret;
    }


    @Override
    public Class<OldBitbucketSynchronizeCsetMsg> getPayloadType()
    {
        return OldBitbucketSynchronizeCsetMsg.class;
    }

}
