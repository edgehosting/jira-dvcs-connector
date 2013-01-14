package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

public class ClientUtils
{

    private static Gson GSON = createGson().create();

    public static final String UTF8 = "UTF-8";

    private static GsonBuilder createGson()
    {
        GsonBuilder builder = new GsonBuilder();
        builder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
        builder.registerTypeAdapter(Date.class, new GsonDateTypeAdapter()); //to parse 2011-12-21 15:17:37

        return builder;
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

    public static <T> T fromJsonWithDeserializationAdapters(InputStream json, Type type, Map<Class<?>, JsonDeserializer<?>> deserializers)
    {
        try
        {
            BufferedReader reader = new BufferedReader(new InputStreamReader(json, UTF8));
            GsonBuilder gson = createGson();
            for (Class<?> forClass : deserializers.keySet())
            {
                gson.registerTypeHierarchyAdapter(forClass, deserializers.get(forClass));
            }
            return gson.create().fromJson(reader, type);
        } catch (Exception e)
        {
            throw new JsonParsingException(e);
        }
    }

    
    private static final class GsonDateTypeAdapter implements JsonDeserializer<Date>
    {

        private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        {
            {
                setTimeZone(TimeZone.getTimeZone("Zulu"));
            }
        };

        @Override
        public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException
        {
            String dateString = json.getAsString();

            try
            {
                return dateFormat.parse(dateString);
            }
            catch (ParseException e)
            {
                throw new JsonParseException("Not parseable datetime string: '" + dateString + "'");
            }
        }
    }
}
