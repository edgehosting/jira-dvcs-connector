package com.atlassian.jira.plugins.dvcs.remoterestpoint;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.IOException;

/**
 * @author Martin Skurla
 */
public class PostCommitHookCallSimulatingRemoteRestpoint
{

    public static int simulate(String jiraInstanceURL, String repositoryId) throws IOException
    {
        String postCommitHookURL =
                String.format("%s/rest/bitbucket/1.0/repository/%s/sync", jiraInstanceURL, repositoryId);

        PostMethod post = new PostMethod(postCommitHookURL);
        NameValuePair[] data =
        {
            new NameValuePair("payload", "fakePayload")
        };
        post.setRequestBody(data);

        HttpClient client = new HttpClient();
        return client.executeMethod(post);
    }
}
