package com.atlassian.jira.plugins.dvcs.service.message;

import com.atlassian.jira.plugins.dvcs.model.DefaultProgress;
import com.atlassian.jira.plugins.dvcs.model.Message;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;
import com.atlassian.jira.util.json.JSONObject;
import org.apache.commons.lang.StringUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.annotation.Resource;

public abstract class AbstractMessagePayloadSerializer<P extends HasProgress> implements MessagePayloadSerializer<P>
{

    @Resource
    private MessagingService messagingService;

    @Resource
    private RepositoryService repositoryService;

    @Resource
    private Synchronizer synchronizer;

    @Override
    public final String serialize(P payload)
    {
        try
        {
            JSONObject json = new JSONObject();

            json.put("repository", payload.getRepository().getId());
            json.put("softSync", payload.isSoftSync());
            json.put("webHookSync", payload.isWebHookSync());
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
            syncAudit = messagingService.getSynchronizationAuditIdFromTags(message.getTags());
            deserialized.repository = repositoryService.get(jsoned.optInt("repository"));

            deserialized.softSync = jsoned.optBoolean("softSync");
            deserialized.webHookSync = jsoned.optBoolean("webHookSync");

            progress = synchronizer.getProgress(deserialized.repository.getId());
            if (progress == null || progress.isFinished())
            {
                progress = new DefaultProgress();
                // inject existing sync audit id
                progress.setAuditLogId(result.getSyncAuditId());
                progress.setSoftsync(deserialized.softSync);
                progress.setWebHookSync(deserialized.webHookSync);
                synchronizer.putProgress(deserialized.repository, progress);
            }
            deserialized.progress = progress;


            deserialized.syncAuditId = syncAudit;

            return result;

        } catch (Exception e)
        {
            throw new MessageDeserializationException(e, progress);
        }
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
            // for payload format version 0 the date format is serialized as formatted string

            String dateStringOrNull = json.optString(dateElement);
            if (StringUtils.isNotBlank(dateStringOrNull))
            {
                date = getDateFormat().parse(dateStringOrNull);
            }

        } else if (version > 0)
        {
            // for payload format version 1 and higher date format is serialized as long
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
