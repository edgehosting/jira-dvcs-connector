package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

/**
 * Need for reflection of hierarchy of
 * {@link BitbucketPullRequestLinks}.
 *
 * 
 * @author mstencel@atlassian.com
 *
 */
public class BitbucketPullRequestLinksDeserializer implements JsonDeserializer<BitbucketPullRequestLinks>
{

    @Override
    public BitbucketPullRequestLinks deserialize(JsonElement json, Type typeOfT,
            JsonDeserializationContext context) throws JsonParseException
    {
    	BitbucketPullRequestLinks links = new BitbucketPullRequestLinks();
    	
        JsonArray jsonArray = json.getAsJsonArray();
        
        for (JsonElement jsonElement : jsonArray)
        {
        	JsonObject jsonObject = jsonElement.getAsJsonObject();
        	String rel = jsonObject.get("rel").getAsString();
        	String href = jsonObject.get("href").getAsString();
        	if ("self".equals(rel))
        	{
        		links.setSelfHref(href);
        	}
        	else if ("html".equals(rel))
        	{
        		links.setHtmlHref(href);
        	}
        	else if ("commits".equals(rel))
        	{
        		links.setCommitsHref(href);
        	}
        	else if ("approvals".equals(rel))
        	{
        		links.setApprovalsHref(href);
        	}
        	else if ("diff".equals(rel))
        	{
        		links.setDiffHref(href);
        	}
        	else if ("comments".equals(rel))
        	{
        		links.setCommentsHref(href);
        	}
        	else if ("activity".equals(rel))
        	{
        		links.setActivityHref(href);
        	}
        }
        
        return links;
    }

    public static Map<Class<?>, JsonDeserializer<?>>  asMap()
    {
        Map<Class<?>, JsonDeserializer<?>> deserializers = new HashMap<Class<?>, JsonDeserializer<?>>();
        deserializers.put(BitbucketPullRequestLinks.class, new BitbucketPullRequestLinksDeserializer());
        return deserializers;
    }
}
