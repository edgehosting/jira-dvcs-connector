package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;

/**
 * 
 * BitbucketChangesetFile
 * 
 * <pre>
 *           "files": [
 *               {
 *                   "type": "modified",
 *                   "file": "piston/oauth.py"
 *               }
 *           ],
 * 
 * </pre>
 *
 * 
 * <br /><br />
 * Created on 12.7.2012, 16:30:43
 * <br /><br />
 * @author jhocman@atlassian.com
 *
 */
public class BitbucketChangesetFile implements Serializable
{
	private static final long serialVersionUID = -5939877483032986046L;
	
	private String type;
	private String file;

	public String getType()
	{
		return type;
	}

	public void setType(String type)
	{
		this.type = type;
	}

	public String getFile()
	{
		return file;
	}

	public void setFile(String file)
	{
		this.file = file;
	}
}
