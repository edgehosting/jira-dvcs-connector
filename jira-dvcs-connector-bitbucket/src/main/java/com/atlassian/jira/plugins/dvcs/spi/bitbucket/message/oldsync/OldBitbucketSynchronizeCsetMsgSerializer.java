package com.atlassian.jira.plugins.dvcs.spi.bitbucket.message.oldsync;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Nullable;

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
import com.google.common.collect.Lists;

/**
 * An implementation of {@link MessagePayloadSerializer} over
 * {@link OldBitbucketSynchronizeCsetMsg}.
 *
 * @author Stanislav Dvorscak
 *
 */
public class OldBitbucketSynchronizeCsetMsgSerializer implements MessagePayloadSerializer<OldBitbucketSynchronizeCsetMsg>
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
    public String serialize(OldBitbucketSynchronizeCsetMsg payload)
    {
        try
        {
            JSONObject result = new JSONObject();
            result.put("branch", payload.getBranch());
            result.put("node", payload.getNode());
            result.put("refreshAfterSynchronizedAt", getDateFormat().format(payload.getRefreshAfterSynchronizedAt()));
            result.put("repository", payload.getRepository().getId());
            result.put("newHeads", Lists.transform(payload.getNewHeads(), new Function<BranchHead, String>()
            {
                @Override
                public String apply(@Nullable BranchHead input)
                {
                    return input.getName() + ":" + input.getHead();
                }
            }));
            result.put("softSync", payload.isSoftSync());
            result.put("syncAuditId", payload.getSyncAuditId());
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
    public OldBitbucketSynchronizeCsetMsg deserialize(String payload)
    {
        Repository repository;
        String branch;
        String node;
        Date refreshAfterSynchronizedAt;
        Progress progress;
        List<BranchHead> newHeads;
        boolean softSync;
        int syncAuditId = 0;

        try
        {
            JSONObject result = new JSONObject(payload);

            repository = repositoryService.get(result.getInt("repository"));
            branch = result.getString("branch");
            node = result.getString("node");
            refreshAfterSynchronizedAt = getDateFormat().parse(result.getString("refreshAfterSynchronizedAt"));
            newHeads = toBranchHeads(result.optJSONArray("newHeads"));
            softSync = result.getBoolean("softSync");
            syncAuditId = result.optInt("syncAuditId");
            new Function<String, BranchHead>()
            {
                @Override
                public BranchHead apply(@Nullable String input)
                {
                    int index = input.lastIndexOf(":");
                    return new BranchHead(input.substring(0, index), input.substring(index + 1));
                }
            };

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

        return new OldBitbucketSynchronizeCsetMsg(repository, branch, node, refreshAfterSynchronizedAt, progress, newHeads, softSync, syncAuditId);
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
    public Class<OldBitbucketSynchronizeCsetMsg> getPayloadType()
    {
        return OldBitbucketSynchronizeCsetMsg.class;
    }

}
