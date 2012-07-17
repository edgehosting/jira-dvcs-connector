package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
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
        String resourceUrl = "/repositories/"+owner+"/"+slug+"/links";
        return (List<RepositoryLink>) bitbucketClient.get(resourceUrl, type);
    }
 
    /**
     * @param owner
     * @param slug
     * @param id
     * @throws BitbucketClientException
     */
    public void removeRepositoryLink(String owner, String slug, int id) throws BitbucketClientException
    {
        bitbucketClient.delete("/repositories/"+owner+"/"+slug+"/links/"+String.valueOf(id));
    }
    
    /**
     * Configures a new Repository Link to the bitbucket repository
     * 
     * @param owner
     * @param slug
     * @param name
     * @param url
     * @param key
     * @return
     * @throws BitbucketClientException
     */
    public RepositoryLink addRepositoryLink(String owner, String slug, String name, String url, String key) throws BitbucketClientException
    {
        Type type = new TypeToken<RepositoryLink>(){}.getType();
        String resourceUrl = "/repositories/"+owner+"/"+slug+"/links";

        List<String> params = Lists.newArrayList();
        params.add("handler=" + name);
        params.add("link_url=" + url);
        params.add("link_key=" + key);
        
        return bitbucketClient.post(resourceUrl, params, type);
    }
    
    public RepositoryLink addCustomRepositoryLink(String owner, String slug, String url) throws BitbucketClientException
    {
        
    	Type type = new TypeToken<RepositoryLink>(){}.getType();
    
        String resourceUrl = "/repositories/" + owner + "/" + slug + "/links";

        List<String> params = Lists.newArrayList();
        params.add("handler=custom");
        params.add("link_url=" + normalize(url) + "\\1");
        try
		{
			params.add("link_key=" + URLEncoder.encode("(?<!\\w)([A-Z|a-z]{2,}-\\d+)(?!\\w)", "UTF-8"));
		} catch (UnsupportedEncodingException e)
		{
			// nop
		}
        
        return bitbucketClient.post(resourceUrl, params, type);
    }
    

	private String normalize(String url)
	{
		if (url.endsWith("/")) {
			return url + "browse/";
		}

		return url + "/browse/";
	}
    

}
