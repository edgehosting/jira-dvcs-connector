package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;
import java.util.List;

/**
 * BitbucketService
 *
 * <pre>
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
public class BitbucketService implements Serializable
{
	private static final long serialVersionUID = 6652570895226582084L;

	private String type;
	private List<BitbucketServiceField> fields;
	
	public String getType()
	{
		return type;
	}

	public void setType(String type)
	{
		this.type = type;
	}

	public List<BitbucketServiceField> getFields()
	{
		return fields;
	}

	public void setFields(List<BitbucketServiceField> fields)
	{
		this.fields = fields;
	}
}
