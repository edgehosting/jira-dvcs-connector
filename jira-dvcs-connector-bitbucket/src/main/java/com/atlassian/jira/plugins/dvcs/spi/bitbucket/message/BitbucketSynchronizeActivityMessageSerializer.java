package com.atlassian.jira.plugins.dvcs.spi.bitbucket.message;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.atlassian.jira.plugins.dvcs.model.DefaultProgress;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.service.message.MessagePayloadSerializer;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;

public class BitbucketSynchronizeActivityMessageSerializer implements MessagePayloadSerializer<BitbucketSynchronizeActivityMessage>
{

    private RepositoryService repositoryService;
    private Synchronizer synchronizer;

    public BitbucketSynchronizeActivityMessageSerializer(RepositoryService repositoryService, Synchronizer synchronizer)
    {
        super();
        this.repositoryService = repositoryService;
        this.synchronizer = synchronizer;
    }

    @Override
    public String serialize(BitbucketSynchronizeActivityMessage payload)
    {

        try
        {
            JSONObject result = new JSONObject();
            result.put("repository", payload.getRepository().getId());
            result.put("softSync", payload.isSoftSync());
            result.put("page", payload.getPageNum());
            result.put("processedPullRequests", payload.getProcessedPullRequests());
            result.put("processedPullRequestsLocal", payload.getProcessedPullRequestsLocal());
            result.put("lastSyncDate", getDateFormat().format(payload.getLastSyncDate()));
            return result.toString();

        } catch (JSONException e)
        {
            throw new RuntimeException(e);

        }
    }

    @Override
    public BitbucketSynchronizeActivityMessage deserialize(String payload)
    {
        Progress progress;
        Repository repository;
        boolean softSync = false;
        Set<Integer> processedPullRequests;
        Set<Integer> processedPullRequestsLocal;
        Date lastSyncDate;
        int page = 1;

        try
        {
            JSONObject result = new JSONObject(payload);

            repository = repositoryService.get(result.optInt("repository"));
            softSync = result.optBoolean("softSync");
            page = result.optInt("page");
            processedPullRequests = asSet(result.optJSONArray("processedPullRequests"));
            processedPullRequestsLocal = asSet(result.optJSONArray("processedPullRequestsLocal"));
            lastSyncDate = getDateFormat().parse(result.getString("lastSyncDate"));

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

        return new BitbucketSynchronizeActivityMessage(repository, progress, softSync, page, processedPullRequests, processedPullRequestsLocal, lastSyncDate);
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

    private DateFormat getDateFormat()
    {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
    }

    @Override
    public Class<BitbucketSynchronizeActivityMessage> getPayloadType()
    {
        return BitbucketSynchronizeActivityMessage.class;
    }

}
