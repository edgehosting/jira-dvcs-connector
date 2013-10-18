package com.atlassian.jira.plugins.dvcs.spi.bitbucket.message;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.commons.collections.CollectionUtils;

import com.atlassian.jira.plugins.dvcs.model.BranchHead;
import com.atlassian.jira.plugins.dvcs.model.DefaultProgress;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.service.message.MessagePayloadSerializer;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
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
public class BitbucketSynchronizeChangesetMessageSerializer implements MessagePayloadSerializer<BitbucketSynchronizeChangesetMessage>
{

    /**
     * @see #setRepositoryService(RepositoryService)
     */
    private RepositoryService repositoryService;

    /**
     * @see #setSynchronizer(Synchronizer)
     */
    private Synchronizer synchronizer;

    /**
     * @param repositoryService
     *            injected {@link RepositoryService} dependency
     */
    public void setRepositoryService(RepositoryService repositoryService)
    {
        this.repositoryService = repositoryService;
    }

    /**
     * @param synchronizer
     *            injected {@link Synchronizer} dependency
     */
    public void setSynchronizer(Synchronizer synchronizer)
    {
        this.synchronizer = synchronizer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String serialize(BitbucketSynchronizeChangesetMessage payload)
    {
        try
        {
            JSONObject result = new JSONObject();
            result.put("refreshAfterSynchronizedAt", getDateFormat().format(payload.getRefreshAfterSynchronizedAt()));
            result.put("repository", payload.getRepository().getId());
            result.put("exclude", collectionToString(payload.getExclude()));
            result.put("page", payload.getPage());
            result.put("syncAuditId", payload.getSyncAuditId());
            result.put("newHeads", Lists.transform(payload.getNewHeads(), new Function<BranchHead, String>()
            {
                @Override
                public String apply(@Nullable BranchHead input)
                {
                    return input.getName() + ":" + input.getHead();
                }
            }));
            result.put("nodesToBranches", payload.getNodesToBranches());
            result.put("softSync", payload.isSoftSync());

            return result.toString();

        } catch (JSONException e)
        {
            throw new RuntimeException(e);

        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BitbucketSynchronizeChangesetMessage deserialize(String payload)
    {
        Repository repository;
        Date refreshAfterSynchronizedAt;
        Progress progress;
        List<BranchHead> newHeads;
        List<String> exclude;
        int page;
        Map<String, String> nodesToBranches;
        boolean softSync;
        int syncAuditId = 0;

        try
        {
            JSONObject result = new JSONObject(payload);

            repository = repositoryService.get(result.optInt("repository"));
            refreshAfterSynchronizedAt = getDateFormat().parse(result.optString("refreshAfterSynchronizedAt"));
            exclude = collectionFromString(result.optString("exclude"));
            page = result.optInt("page");
            syncAuditId = result.optInt("syncAuditId");
            newHeads = toBranchHeads(result.optJSONArray("newHeads"));
            softSync = result.getBoolean("softSync");
            new Function<String, BranchHead>()
            {
                @Override
                public BranchHead apply(@Nullable String input)
                {
                    int index = input.lastIndexOf(":");
                    return new BranchHead(input.substring(0, index), input.substring(index + 1));
                }
            };
            nodesToBranches = asMap(result.optJSONObject("nodesToBranches"));

            progress = synchronizer.getProgress(repository.getId());
            if (progress == null || progress.isFinished())
            {
                synchronizer.putProgress(repository, progress = new DefaultProgress());
            }

        } catch (JSONException e)
        {
            throw new RuntimeException(e);

        } catch (ParseException e)
        {
            throw new RuntimeException(e);

        }

        return new BitbucketSynchronizeChangesetMessage(repository, refreshAfterSynchronizedAt, progress, newHeads, exclude,
                 page, nodesToBranches, softSync, syncAuditId);
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

    /**
     * @return date formatter
     */
    private DateFormat getDateFormat()
    {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
    }

    /**
     * {@inheritDoc}
     */
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
