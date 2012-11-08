package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ClientUtils
{

    private static Gson GSON = createGson();

    public static final String UTF8 = "UTF-8";

    private static Gson createGson()
    {
        GsonBuilder builder = new GsonBuilder();
        builder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
        builder.setDateFormat("yyyy-MM-dd HH:mm:ss"); // to parse 2011-12-21
                                                      // 15:17:37
        return builder.create();
    }

    public static String toJson(Object object)
    {
        try
        {
            return GSON.toJson(object);
        } catch (Exception e)
        {
            throw new JsonParsingException(e);
        }
    }

    public static <T> T fromJson(String json, Class<T> type)
    {
        try
        {
            return GSON.fromJson(json, type);
        } catch (Exception e)
        {
            throw new JsonParsingException(e);
        }
    }

    public static <T> T fromJson(InputStream json, Class<T> type)
    {
        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader(new InputStreamReader(json, UTF8));
            return GSON.fromJson(reader, type);
        } catch (Exception e)
        {
            throw new JsonParsingException(e);
        } 
    }

    public static <T> T fromJson(InputStream json, Type type)
    {
        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader(new InputStreamReader(json, UTF8));
            return GSON.fromJson(reader, type);
        } catch (Exception e)
        {
            throw new JsonParsingException(e);
        }
    }
}
