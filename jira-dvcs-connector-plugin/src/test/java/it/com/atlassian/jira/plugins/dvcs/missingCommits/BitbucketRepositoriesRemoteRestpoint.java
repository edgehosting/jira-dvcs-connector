package it.com.atlassian.jira.plugins.dvcs.missingCommits;

import java.util.HashMap;
import java.util.Map;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteRequestor;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.ResponseCallback;

/**
 * @author Martin Skurla
 */
public class BitbucketRepositoriesRemoteRestpoint
{
    private final RemoteRequestor requestor;

    public BitbucketRepositoriesRemoteRestpoint(RemoteRequestor requestor)
    {
        this.requestor = requestor;
    }

    public void createHgRepository(String repositoryName)
    {
        createRepository(repositoryName, "hg");
    }

    private void createRepository(String repositoryName, String scm)
    {
        Map<String, String> createRepoPostData = new HashMap<String, String>();
        createRepoPostData.put("name", repositoryName);
        createRepoPostData.put("scm",  scm);

        requestor.post("/repositories", createRepoPostData, ResponseCallback.EMPTY);
    }

    public void removeExistingRepository(String repositoryName, String owner)
    {
        Map<String, String> removeRepoPostData = new HashMap<String, String>();
        removeRepoPostData.put("repo_slug",   repositoryName);
        removeRepoPostData.put("accountname", owner);

        String removeRepositoryUrl = String.format("/repositories/%s/%s", owner, repositoryName);

        requestor.delete(removeRepositoryUrl, removeRepoPostData, ResponseCallback.EMPTY);
    }
}
