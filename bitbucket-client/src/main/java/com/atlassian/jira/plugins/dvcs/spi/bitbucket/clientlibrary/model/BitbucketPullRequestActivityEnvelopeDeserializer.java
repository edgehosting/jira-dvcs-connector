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
 */
public class BitbucketPullRequestActivityEnvelopeDeserializer implements JsonDeserializer<BitbucketPullRequestActivityInfo>
{

    @Override
    public BitbucketPullRequestActivityInfo deserialize(JsonElement json, Type typeOfT,
            JsonDeserializationContext context) throws JsonParseException
    {
        
        JsonObject jsonObject = json.getAsJsonObject();

        BitbucketPullRequestActivityInfo info = new BitbucketPullRequestActivityInfo();
        info.setPullRequest((BitbucketPullRequest) context.deserialize(jsonObject.get("pull_request"), BitbucketPullRequest.class));

        BitbucketPullRequestBaseActivity activity = null;

        if (asComment(jsonObject) != null)
        {
            activity = context.deserialize(asComment(jsonObject), BitbucketPullRequestCommentActivity.class);

        } else if (asUpdate(jsonObject) != null)
        {
            activity =  context.deserialize(asUpdate(jsonObject), BitbucketPullRequestUpdateActivity.class);

        } else if (asLike(jsonObject) != null)
        {
            activity =  context.deserialize(asLike(jsonObject), BitbucketPullRequestApprovalActivity.class);

        } else {
            
            throw new JsonParseException("Unknown type of activity : " + json.getAsString());
        }
        
        info.setActivity(activity);
        
        return info;

    }

    public static Map<Class<?>, JsonDeserializer<?>>  asMap()
    {
        Map<Class<?>, JsonDeserializer<?>> deserializers = new HashMap<Class<?>, JsonDeserializer<?>>();
        deserializers.put(BitbucketPullRequestActivityInfo.class, new BitbucketPullRequestActivityEnvelopeDeserializer ());
        return deserializers;
    }

    private JsonElement asComment(JsonObject jsonObject)
    {
        return jsonObject.get("comment");
    }
    private JsonElement asLike(JsonObject jsonObject)
    {
        return jsonObject.get("approval");
    }
    private JsonElement asUpdate(JsonObject jsonObject)
    {
        return jsonObject.get("update");
    }

}
