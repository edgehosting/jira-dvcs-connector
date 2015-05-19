package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Need for reflection of hierarchy of {@link BitbucketPullRequestBaseActivity}.
 *
 * @author jhocman@atlassian.com
 */
public class BitbucketPullRequestActivityDeserializer implements JsonDeserializer<BitbucketPullRequestBaseActivity>
{

    @Override
    public BitbucketPullRequestBaseActivity deserialize(JsonElement json, Type typeOfT,
            JsonDeserializationContext context) throws JsonParseException
    {
        
        JsonObject jsonObject = json.getAsJsonObject();

        if (asComment(jsonObject) != null)
        {
            return context.deserialize(asComment(jsonObject), BitbucketPullRequestCommentActivity.class);

        } else if (asUpdate(jsonObject) != null)
        {
            return context.deserialize(asUpdate(jsonObject), BitbucketPullRequestUpdateActivity.class);

        } else if (asApproval(jsonObject) != null)
        {
            return context.deserialize(asApproval(jsonObject), BitbucketPullRequestApprovalActivity.class);
        }

        throw new JsonParseException("Unknown type of activity : " + json);
    }

    public static Map<Class<?>, JsonDeserializer<?>>  asMap()
    {
        Map<Class<?>, JsonDeserializer<?>> deserializers = new HashMap<Class<?>, JsonDeserializer<?>>();
        deserializers.put(BitbucketPullRequestBaseActivity.class, new BitbucketPullRequestActivityDeserializer ());
        return deserializers;
    }

    private JsonElement asComment(JsonObject jsonObject)
    {
        return jsonObject.get("comment");
    }
    private JsonElement asApproval(JsonObject jsonObject)
    {
        return jsonObject.get("approval");
    }
    private JsonElement asUpdate(JsonObject jsonObject)
    {
        return jsonObject.get("update");
    }

}
