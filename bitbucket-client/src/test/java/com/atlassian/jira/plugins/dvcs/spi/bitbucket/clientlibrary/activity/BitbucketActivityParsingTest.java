package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.activity;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.testng.annotations.Test;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestActivityEnvelopeDeserializer;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestActivityInfo;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestBaseActivityEnvelope;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/**
 * Sandbox - will be deleted
 * 
 */
public class BitbucketActivityParsingTest
{

    public BitbucketActivityParsingTest()
    {
        super();
    }

    @Test
    public void testParseSuccess() throws IOException
    {

        InputStream stream = BitbucketActivityParsingTest.class.getResourceAsStream("sampleActivity.json");
        Map<Class<?>, JsonDeserializer<?>> deserializers = new HashMap<Class<?>, JsonDeserializer<?>>();
        deserializers.put(BitbucketPullRequestActivityInfo.class,
                new BitbucketPullRequestActivityEnvelopeDeserializer());

        GsonBuilder g = new GsonBuilder();
        g.registerTypeHierarchyAdapter(BitbucketPullRequestActivityInfo.class,
                new BitbucketPullRequestActivityEnvelopeDeserializer());

        JsonReader reader = new JsonReader(new InputStreamReader(stream));

        Object fromJson = g.create().fromJson(reader, BitbucketPullRequestBaseActivityEnvelope.class);
        System.out.println(fromJson);
        /*
         * Object object = g.create().fromJson(reader, new
         * TypeToken<BitbucketPullRequestBaseActivityEnvelope>() { }.getType());
         *//*
            * while (reader.hasNext()) { }
            */
        /*
         * reader.endArray(); reader.close();
         */

        /*
         * reader.beginArray();
         * 
         * int i = 0; while (reader.peek() !=
         * com.google.gson.stream.JsonToken.END_DOCUMENT) {
         * ByteArrayOutputStream baos = new ByteArrayOutputStream(); JsonWriter
         * out = new JsonWriter(new OutputStreamWriter(baos));
         * out.setIndent("    "); Stack stack = new Stack(); prettyprint(reader,
         * out, 1, stack); i++; System.out.println(">>> " + i); out.endObject();
         * out.endObject(); out.close(); //reader.close();
         * System.out.println(baos.toString()); }
         * 
         * reader.endArray();
         */

        // stack.clear();
        // prettyprint(reader, out, 2, stack);
        // prettyprint(reader, out, 2, stack);

    }

    static void prettyprint(JsonReader reader, JsonWriter writer, int depth, Stack stack) throws IOException
    {
        int level = 0;
        while (level != depth || stack.isEmpty())
        {
            JsonToken token = reader.peek();
            switch (token)
            {
            case BEGIN_ARRAY:
                reader.beginArray();
                writer.beginArray();
                break;
            case END_ARRAY:
                reader.endArray();
                writer.endArray();
                break;

            case BEGIN_OBJECT:
                level++;
                reader.beginObject();
                writer.beginObject();
                if (level > depth)
                {
                    System.out.println(level + "pushing");
                    stack.push(new Object());
                }
                break;

            case END_OBJECT:
                reader.endObject();
                writer.endObject();
                level--;
                if (level <= depth && !stack.isEmpty())
                {
                    System.out.println(level + "popin");
                    stack.pop();
                }
                System.out.println("EO");
                break;

            case NAME:
                String name = reader.nextName();
                writer.name(name);
                break;
            case STRING:
                String s = reader.nextString();
                writer.value(s);
                break;
            case NUMBER:
                String n = reader.nextString();
                writer.value(new BigDecimal(n));
                break;
            case BOOLEAN:
                boolean b = reader.nextBoolean();
                writer.value(b);
                break;
            case NULL:
                reader.nextNull();
                writer.nullValue();
                break;
            case END_DOCUMENT:
                return;
            }
        }
    }
}
