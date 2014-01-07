package com.atlassian.jira.plugins.dvcs.spi.github.message;

import java.util.Date;

import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.service.message.AbstractMessagePayloadSerializer;
import com.atlassian.jira.plugins.dvcs.service.message.MessagePayloadSerializer;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;
import com.atlassian.jira.util.json.JSONObject;

/**
 * An implementation of {@link MessagePayloadSerializer} over {@link SynchronizeChangesetMessage}.
 *
 * @author Stanislav Dvorscak
 *
 */
public class SynchronizeChangesetMessageSerializer extends AbstractMessagePayloadSerializer<SynchronizeChangesetMessage>
{

    public SynchronizeChangesetMessageSerializer(RepositoryService repositoryService, Synchronizer synchronizer)
    {
        super(repositoryService, synchronizer);
    }

    @Override
    protected void serializeInternal(JSONObject json, SynchronizeChangesetMessage payload) throws Exception
    {
        json.put("branch", payload.getBranch());
        json.put("node", payload.getNode());
        json.put("refreshAfterSynchronizedAt", payload.getRefreshAfterSynchronizedAt().getTime());
    }

    @Override
    protected SynchronizeChangesetMessage deserializeInternal(JSONObject json, final int version) throws Exception
    {
        String branch;
        String node;
        Date refreshAfterSynchronizedAt;

        branch = json.getString("branch");
        node = json.getString("node");
        refreshAfterSynchronizedAt = parseDate(json, "refreshAfterSynchronizedAt", version);

        return new SynchronizeChangesetMessage(null, branch, node, refreshAfterSynchronizedAt, null, false, 0);
    }

    @Override
    public Class<SynchronizeChangesetMessage> getPayloadType()
    {
        return SynchronizeChangesetMessage.class;
    }


}
