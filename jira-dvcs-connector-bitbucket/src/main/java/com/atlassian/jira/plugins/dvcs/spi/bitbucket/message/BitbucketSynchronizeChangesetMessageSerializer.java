package com.atlassian.jira.plugins.dvcs.spi.bitbucket.message;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.commons.collections.CollectionUtils;

import com.atlassian.jira.plugins.dvcs.model.BranchHead;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.service.message.AbstractMessagePayloadSerializer;
import com.atlassian.jira.plugins.dvcs.service.message.MessagePayloadSerializer;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONObject;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

/**
 * An implementation of {@link MessagePayloadSerializer} over {@link BitbucketSynchronizeChangesetMessage}.
 *
 * @author Stanislav Dvorscak
 *
 */
public class BitbucketSynchronizeChangesetMessageSerializer extends AbstractMessagePayloadSerializer<BitbucketSynchronizeChangesetMessage>
{

    public BitbucketSynchronizeChangesetMessageSerializer(RepositoryService repositoryService, Synchronizer synchronizer)
    {
        super(repositoryService, synchronizer);
    }

    @Override
    protected void serializeInternal(JSONObject json, BitbucketSynchronizeChangesetMessage payload) throws Exception
    {
        json.put("refreshAfterSynchronizedAt", getDateFormat().format(payload.getRefreshAfterSynchronizedAt()));
        json.put("exclude", collectionToString(payload.getExclude()));
        json.put("page", payload.getPage());
        json.put("include", collectionToString(payload.getInclude()));
        json.put("nodesToBranches", payload.getNodesToBranches());
    }

    @Override
    protected BitbucketSynchronizeChangesetMessage deserializeInternal(JSONObject json) throws Exception
    {
        Date refreshAfterSynchronizedAt;
        List<String> exclude;
        List<String> include;
        int page;
        Map<String, String> nodesToBranches;

        refreshAfterSynchronizedAt = getDateFormat().parse(json.optString("refreshAfterSynchronizedAt"));
        exclude = collectionFromString(json.optString("exclude"));
        page = json.optInt("page");
        include = collectionFromString(json.optString("include"));
        nodesToBranches = asMap(json.optJSONObject("nodesToBranches"));

        return new BitbucketSynchronizeChangesetMessage(null, refreshAfterSynchronizedAt, null, include, exclude,
                page, nodesToBranches, false, 0);
    }

    protected Map<String, String> asMap(JSONObject object)
    {
        String[] names = JSONObject.getNames(object);
        Map<String, String> ret = new HashMap<String, String>();
        for (String keyName : names)
        {
            ret.put(keyName, object.optString(keyName));
        }
        return ret;
    }

    @Override
    public Class<BitbucketSynchronizeChangesetMessage> getPayloadType()
    {
        return BitbucketSynchronizeChangesetMessage.class;
    }

    private String collectionToString(List<String> coll)
    {
        return CollectionUtils.isEmpty(coll) ? null : Joiner.on(",").join(coll);
    }

    private ArrayList<String> collectionFromString(String string)
    {
        return string == null ? Lists.<String> newArrayList() : Lists.newArrayList(Splitter.on(",").split(string));
    }

}
