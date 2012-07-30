package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary;

import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.gson.reflect.TypeToken;

/**
 * Java client for Bitbucket's Repository links API:
 *      http://confluence.atlassian.com/display/BITBUCKET/Repository+links
 */
public class RepositoryLinksService
{
    private final BitbucketClient bitbucketClient;

    public RepositoryLinksService(BitbucketClient bitbucketClient)
    {
        this.bitbucketClient = bitbucketClient;
    }

    /**
     * @param owner
     * @param slug
     * @return
     * @throws BitbucketClientException
     */
    @SuppressWarnings("unchecked")
    public List<RepositoryLink> getRepositoryLinks(String owner, String slug) throws BitbucketClientException
    {
        Type type = new TypeToken<List<RepositoryLink>>(){}.getType();

        String resourceUrl = MessageFormat.format("/repositories/{0}/{1}/links", owner, slug);
        return (List<RepositoryLink>) bitbucketClient.get(resourceUrl, type);
    }
    
    /**
     * Gets Repository Link frm given owner, slug and repository link id.
     * 
     * @param owner
     * @param slug
     * @param id
     * @return
     * @throws BitbucketClientException 
     */
    public RepositoryLink getRepositoryLink(String owner, String slug, int id) throws BitbucketClientException
    {
        Type type = new TypeToken<RepositoryLink>(){}.getType();
        
        String resourceUrl = MessageFormat.format("/repositories/{0}/{1}/links/{2}", owner, slug, String.valueOf(id));
        
        return (RepositoryLink) bitbucketClient.get(resourceUrl, type);
    }

    /**
     * @param owner
     * @param slug
     * @param id
     * @throws BitbucketClientException
     */
    public void removeRepositoryLink(String owner, String slug, int id) throws BitbucketClientException
    {
        String resourceUrl = MessageFormat.format("/repositories/{0}/{1}/links/{2}", owner, slug, String.valueOf(id));

        bitbucketClient.delete(resourceUrl);
    }

    /**
     * Configures a new Repository Link to the bitbucket repository
     *
     * @param owner
     * @param slug
     * @param handler
     * @param linkUrl
     * @param linkKey
     * @return
     * @throws BitbucketClientException
     */
    public RepositoryLink addRepositoryLink(String owner, String slug, String handler, String linkUrl, String linkKey) throws BitbucketClientException
    {
        Type type = new TypeToken<RepositoryLink>(){}.getType();

        String resourceUrl = MessageFormat.format("/repositories/{0}/{1}/links", owner, slug);

        List<String> params = Lists.newArrayList();
        params.add("handler=" + handler);
        params.add("link_url=" + linkUrl);
        params.add("link_key=" + linkKey);

        return (RepositoryLink) bitbucketClient.post(resourceUrl, params, type);
    }

}
