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
 * 
 * <br /><br />
 * Created on 12.7.2012, 17:00:19
 * <br /><br />
 * @author jhocman@atlassian.com
 *
 */
public class BitbucketService implements Serializable
{

	private static final long serialVersionUID = 6652570895226582084L;

	private String type;
	
	private List<BitbucketServiceField> fields;
	
	public BitbucketService()
	{
		super();
	}

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

