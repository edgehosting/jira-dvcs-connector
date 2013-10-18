package com.atlassian.jira.plugins.dvcs.service.message;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import com.atlassian.jira.plugins.dvcs.model.DefaultProgress;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;
import com.atlassian.jira.util.json.JSONObject;

public abstract class AbstractMessagePayloadSerializer<P extends HasProgress> implements MessagePayloadSerializer<P>
{
    private RepositoryService repositoryService;
    private Synchronizer synchronizer;

    protected AbstractMessagePayloadSerializer(RepositoryService repositoryService, Synchronizer synchronizer)
    {
        super();
        this.repositoryService = repositoryService;
        this.synchronizer = synchronizer;
    }

    @Override
    public final String serialize(P payload)
    {

        try
        {
            JSONObject json = new JSONObject();

            json.put("repository", payload.getRepository().getId());
            json.put("softSync", payload.isSoftSync());
            json.put("syncAuditId", payload.getSyncAuditId());
            //
            serializeInternal(json, payload);
            //
            return json.toString();
            //
        } catch (Exception e)
        {
            throw new MessageSerializationException(e, payload);
        }
    }

    @Override
    public final P deserialize(int messageId, String payload, int repoId)
    {
        Progress progress = null;
        try
        {
            JSONObject jsoned = new JSONObject(payload);

            P result = deserializeInternal(jsoned);
            //
            BaseProgressEnabledMessage deserialized = (BaseProgressEnabledMessage) result;
            deserialized.repository = repositoryService.get(repoId);
            deserialized.softSync = jsoned.optBoolean("softSync");
            deserialized.syncAuditId = jsoned.optInt("syncAuditId");

            //
            progress = synchronizer.getProgress(deserialized.repository.getId());
            if (progress == null || progress.isFinished())
            {
                progress = new DefaultProgress();
                synchronizer.putProgress(deserialized.repository, progress);
            }
            deserialized.progress = progress;
            //
            return result;

        } catch (Exception e)
        {
            throw new MessageDeserializationException(e, progress, messageId);
        }
    }

    protected abstract void serializeInternal(JSONObject json, P payload) throws Exception;
    protected abstract P deserializeInternal(JSONObject json) throws Exception;

    protected final DateFormat getDateFormat()
    {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
    }

    public static class MessageDeserializationException extends RuntimeException
    {

        private static final long serialVersionUID = -469226983071844241L;

        private Progress progress;
        private int messageId;

        private MessageDeserializationException(Throwable cause, Progress p, int messageId)
        {
            super(cause);
            this.progress = p;
            this.messageId = messageId;
        }

        public Progress getProgressOrNull()
        {
            return progress;
        }

        public int getMessageId()
        {
            return messageId;
        }
    }

    public static class MessageSerializationException extends RuntimeException
    {

        private static final long serialVersionUID = -469226983071844241L;

        private HasProgress message;

        private MessageSerializationException(Throwable cause, HasProgress msg)
        {
            super(cause);
            this.message = msg;
        }

        public HasProgress getSerializedMessage()
        {
            return message;
        }
    }

}
