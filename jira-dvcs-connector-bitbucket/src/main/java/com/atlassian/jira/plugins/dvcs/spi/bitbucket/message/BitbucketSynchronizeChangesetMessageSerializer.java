package com.atlassian.jira.plugins.dvcs.spi.bitbucket.message;

import com.atlassian.jira.plugins.dvcs.service.message.AbstractMessagePayloadSerializer;
import com.atlassian.jira.plugins.dvcs.service.message.MessagePayloadSerializer;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangesetPage;
import com.atlassian.jira.util.json.JSONObject;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An implementation of {@link MessagePayloadSerializer} over {@link BitbucketSynchronizeChangesetMessage}.
 *
 * @author Stanislav Dvorscak
 */
@Component
public class BitbucketSynchronizeChangesetMessageSerializer
        extends AbstractMessagePayloadSerializer<BitbucketSynchronizeChangesetMessage>
{

    @Override
    protected void serializeInternal(JSONObject json, BitbucketSynchronizeChangesetMessage payload) throws Exception
    {
        json.put("refreshAfterSynchronizedAt", payload.getRefreshAfterSynchronizedAt().getTime());
        if (payload.getExclude() != null && !payload.getExclude().isEmpty())
        {
            json.put("exclude", collectionToString(payload.getExclude()));
        }
        if (payload.getInclude() != null && !payload.getInclude().isEmpty())
        {
            json.put("include", collectionToString(payload.getInclude()));
        }
        if (payload.getPage() != null)
        {
            json.put("nextPage", payload.getPage().getNext());
            json.put("page", payload.getPage().getPage());
        }
        json.put("nodesToBranches", payload.getNodesToBranches());
    }

    @Override
    protected BitbucketSynchronizeChangesetMessage deserializeInternal(JSONObject json, final int version)
            throws Exception
    {
        Date refreshAfterSynchronizedAt;
        List<String> exclude;
        List<String> include;
        BitbucketChangesetPage page = null;
        Map<String, String> nodesToBranches;

        refreshAfterSynchronizedAt = parseDate(json, "refreshAfterSynchronizedAt", version);
        exclude = collectionFromString(json.optString("exclude"));
        page = new BitbucketChangesetPage();

        page.setNext(json.optString("nextPage"));
        page.setPage(json.optInt("page"));

        include = collectionFromString(json.optString("include"));
        nodesToBranches = asMap(json.optJSONObject("nodesToBranches"));

        return new BitbucketSynchronizeChangesetMessage(null, refreshAfterSynchronizedAt, null, include, exclude, page, nodesToBranches, false, 0, false);
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
        return StringUtils.isBlank(string) ? Lists.<String>newArrayList() : Lists.newArrayList(Splitter.on(",").split(string));
    }

}
