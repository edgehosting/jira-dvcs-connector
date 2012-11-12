package it.com.atlassian.jira.plugins.dvcs.missingCommits;

import java.io.IOException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;

/**
 * @author Martin Skurla
 */
public class PostCommitHookCallSimulatingRemoteRestpoint
{

	public static int simulate(String repositoryId) throws IOException
    {
        String postCommitHookURL =
                String.format("http://localhost:2990/jira/rest/bitbucket/1.0/repository/%s/sync", repositoryId);
        
        PostMethod post = new PostMethod(postCommitHookURL);
        NameValuePair[] data = {
            new NameValuePair("payload", "fakePayload")
        };
        post.setRequestBody(data);

        HttpClient client = new HttpClient();
        return client.executeMethod(post);
	}
}
