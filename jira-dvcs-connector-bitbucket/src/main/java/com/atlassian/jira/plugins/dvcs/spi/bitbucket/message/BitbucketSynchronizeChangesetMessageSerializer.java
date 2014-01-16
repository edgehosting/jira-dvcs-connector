package com.atlassian.jira.plugins.dvcs.spi.bitbucket.message;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangesetPage;
import org.apache.commons.collections.CollectionUtils;

import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.service.message.AbstractMessagePayloadSerializer;
import com.atlassian.jira.plugins.dvcs.service.message.MessagePayloadSerializer;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;
import com.atlassian.jira.util.json.JSONObject;
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
        json.put("refreshAfterSynchronizedAt", payload.getRefreshAfterSynchronizedAt().getTime());
        json.put("exclude", collectionToString(payload.getExclude()));
        // We only need next page
        json.put("nextPage", payload.getPage().getNext());
        json.put("include", collectionToString(payload.getInclude()));
    }

    @Override
    protected BitbucketSynchronizeChangesetMessage deserializeInternal(JSONObject json, final int version) throws Exception
    {
        Date refreshAfterSynchronizedAt;
        List<String> exclude;
        List<String> include;
        BitbucketChangesetPage page;

        refreshAfterSynchronizedAt = parseDate(json, "refreshAfterSynchronizedAt", version);
        exclude = collectionFromString(json.optString("exclude"));
        page = new BitbucketChangesetPage();
        page.setNext(json.optString("nextPage"));
        include = collectionFromString(json.optString("include"));

        return new BitbucketSynchronizeChangesetMessage(null, refreshAfterSynchronizedAt, null, include, exclude, page, false, 0);
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
