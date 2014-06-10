package com.atlassian.jira.plugins.dvcs.spi.bitbucket.message.oldsync;

import com.atlassian.jira.plugins.dvcs.service.message.AbstractMessagePayloadSerializer;
import com.atlassian.jira.plugins.dvcs.service.message.MessagePayloadSerializer;
import com.atlassian.jira.util.json.JSONObject;

import java.util.Date;

/**
 * An implementation of {@link MessagePayloadSerializer} over
 * {@link OldBitbucketSynchronizeCsetMsg}.
 *
 * @author Stanislav Dvorscak
 *
 */
public class OldBitbucketSynchronizeCsetMsgSerializer extends AbstractMessagePayloadSerializer<OldBitbucketSynchronizeCsetMsg>
{

    @Override
    protected void serializeInternal(JSONObject json, OldBitbucketSynchronizeCsetMsg payload) throws Exception
    {
        json.put("branch", payload.getBranch());
        json.put("node", payload.getNode());
        json.put("refreshAfterSynchronizedAt", payload.getRefreshAfterSynchronizedAt().getTime());
    }

    @Override
    protected OldBitbucketSynchronizeCsetMsg deserializeInternal(JSONObject json, final int version) throws Exception
    {
        String branch;
        String node;
        Date refreshAfterSynchronizedAt;

        branch = json.getString("branch");
        node = json.getString("node");
        refreshAfterSynchronizedAt = parseDate(json, "refreshAfterSynchronizedAt", version);

        return new OldBitbucketSynchronizeCsetMsg(null, branch, node, refreshAfterSynchronizedAt, null, false, 0, false);
    }

    @Override
    public Class<OldBitbucketSynchronizeCsetMsg> getPayloadType()
    {
        return OldBitbucketSynchronizeCsetMsg.class;
    }

}
