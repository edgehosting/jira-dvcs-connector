package com.atlassian.jira.plugins.dvcs.spi.bitbucket.message;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import com.atlassian.jira.plugins.dvcs.model.DefaultProgress;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.service.message.MessagePayloadSerializer;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;
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

        try
        {
            JSONObject result = new JSONObject(payload);

            repository = repositoryService.get(result.optInt("repository"));

            progress = synchronizer.getProgress(repository.getId());
            if (progress == null || progress.isFinished())
            {
                synchronizer.putProgress(repository, progress = new DefaultProgress());
            }

        } catch (JSONException e)
        {
            throw new RuntimeException(e);

        }

        return new BitbucketSynchronizeActivityMessage(repository, progress, false, null, null);
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
