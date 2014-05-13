package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestActivityEnvelopeDeserializer;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestActivityInfo;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestBaseActivity;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class ClientUtils
{

    private static Gson GSON = createGson();

    public static final String UTF8 = "UTF-8";

    private static Gson createGson()
    {
        GsonBuilder builder = new GsonBuilder();
        builder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
        builder.registerTypeAdapter(Date.class, new GsonDateTypeAdapter()); //to parse 2011-12-21 15:17:37
        builder.registerTypeAdapter(BitbucketPullRequestActivityInfo.class, new BitbucketPullRequestActivityEnvelopeDeserializer ());

        return builder.create();
    }

    public static String toJson(Object object)
    {
        try
        {
            return GSON.toJson(object);
        }
        catch (Exception e)
        {
            throw new JsonParsingException(e);
        }
    }

    public static <T> T fromJson(String json, Class<T> type)
    {
        try
        {
            return GSON.fromJson(json, type);
        }
        catch (Exception e)
        {
            throw new JsonParsingException(e);
        }
    }

    public static <T> T fromJson(InputStream json, Class<T> type)
    {
        try
        {
            BufferedReader reader = new BufferedReader(new InputStreamReader(json, UTF8));
            return GSON.fromJson(reader, type);
        } catch (Exception e)
        {
            throw new JsonParsingException(e);
        }
    }

    public static <T> T fromJson(InputStream json, Type type)
    {
        try
        {
            BufferedReader reader = new BufferedReader(new InputStreamReader(json, UTF8));
            return GSON.fromJson(reader, type);
        } catch (Exception e)
        {
            throw new JsonParsingException(e);
        }
    }

    private static final class GsonDateTypeAdapter implements JsonDeserializer<Date>
    {

        private final DateFormat[] dateFormats = new DateFormat[] { 
        		new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
		        {
		            {
		                setTimeZone(TimeZone.getTimeZone("Zulu"));
		            }
		        },
		        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
		        {
		            {
		                setTimeZone(TimeZone.getTimeZone("Zulu"));
		            }
		        }
        };
        
        @Override
        public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException
        {
            String dateString = json.getAsString();
            
            // we need to synchronize SimpleDateFormat as it is not thread-safe
            // we could also use ThreadLocal to improve performance here
            RuntimeException exception = null;
            for ( DateFormat dateFormat : dateFormats)
        	{
	            try
	            {
	                synchronized (dateFormat)
	                {
	                    return dateFormat.parse(dateString);
	                }
	            	
                } catch (ParseException e)
                {
                    exception = new JsonParseException("Not parseable datetime string: '" + dateString + "'");
                }
        	}
            
            throw exception;
        }
    }
    
    public static Date extractActivityDate(BitbucketPullRequestBaseActivity activity)
    {
        Date date = activity.getUpdatedOn();
        // fallbacks - order depends
        if (date == null)
        {
            date = activity.getDate();
        }
        if (date == null)
        {
            date = activity.getCreatedOn();
        }
        return date;
    }
}
