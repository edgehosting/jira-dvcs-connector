package com.atlassian.jira.plugins.dvcs.service.message;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang.StringUtils;

import com.atlassian.jira.plugins.dvcs.model.DefaultProgress;
import com.atlassian.jira.plugins.dvcs.model.Message;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;
import com.atlassian.jira.util.json.JSONObject;

public abstract class AbstractMessagePayloadSerializer<P extends HasProgress> implements MessagePayloadSerializer<P>
{
    public static final String SYNCHRONIZATION_AUDIT_TAG_PREFIX = "audit-id-";

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
            json.put("version", 1);
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
    public final P deserialize(Message<P> message)
    {
        Progress progress = null;
        int syncAudit = 0;
        try
        {
            JSONObject jsoned = new JSONObject(message.getPayload());

            int version = jsoned.optInt("version", 0);
            P result = deserializeInternal(jsoned, version);
            //
            BaseProgressEnabledMessage deserialized = (BaseProgressEnabledMessage) result;

            // progress stuff
            //
            syncAudit = getSyncAuditIdFromTags(message.getTags());
            deserialized.repository = repositoryService.get(jsoned.optInt("repository"));

            progress = synchronizer.getProgress(deserialized.repository.getId());
            if (progress == null || progress.isFinished())
            {
                progress = new DefaultProgress();
                // inject existing sync audit id
                progress.setAuditLogId(result.getSyncAuditId());
                synchronizer.putProgress(deserialized.repository, progress);
            }
            deserialized.progress = progress;

            deserialized.softSync = jsoned.optBoolean("softSync");
            deserialized.syncAuditId = syncAudit;

            return result;

        } catch (Exception e)
        {
            throw new MessageDeserializationException(e, progress);
        }
    }

    public static <PR extends HasProgress> int getSyncAuditIdFromTags(String[] tags)
    {
        for (String tag : tags)
        {
            if (StringUtils.startsWith(tag, SYNCHRONIZATION_AUDIT_TAG_PREFIX))
            {
                return Integer.parseInt(tag.substring(SYNCHRONIZATION_AUDIT_TAG_PREFIX.length()));

            }
        }
        return 0;
    }

    protected abstract void serializeInternal(JSONObject json, P payload) throws Exception;
    protected abstract P deserializeInternal(JSONObject json, int version) throws Exception;

    protected final DateFormat getDateFormat()
    {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
    }

    protected final Date parseDate(JSONObject json, String dateElement, int version) throws ParseException
    {
        Date date = null;
        if (version == 0)
        {
            String dateStringOrNull = json.optString(dateElement);
            if (StringUtils.isNotBlank(dateStringOrNull))
            {
                date = getDateFormat().parse(dateStringOrNull);
            }

        } else if (version > 0)
        {
           date = new Date(json.optLong(dateElement));
        }

        return date;
    }

    public static class MessageDeserializationException extends RuntimeException
    {

        private static final long serialVersionUID = -469226983071844241L;

        private Progress progress;

        private MessageDeserializationException(Throwable cause, Progress p)
        {
            super(cause);
            this.progress = p;
        }

        public Progress getProgressOrNull()
        {
            return progress;
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
