package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;

/**
 * BitbucketServiceEnvelope
 *
 * <pre>
 * "id": 5,
 *    "service": {
 *       "fields": [
 *           {
 *               "name": "URL",
 *               "value": "https://bitbucket.org/post"
 *           }
 *       ],
 *       "type": "POST"
 *   }
 * </pre>
 */
public class BitbucketServiceEnvelope implements Serializable
{
	private static final long serialVersionUID = -4793873691309696707L;

	private Integer id;
	private BitbucketService service;
	
	public Integer getId()
	{
		return id;
	}

	public void setId(Integer id)
	{
		this.id = id;
	}

	public BitbucketService getService()
	{
		return service;
	}

	public void setService(BitbucketService service)
	{
		this.service = service;
	}
}
