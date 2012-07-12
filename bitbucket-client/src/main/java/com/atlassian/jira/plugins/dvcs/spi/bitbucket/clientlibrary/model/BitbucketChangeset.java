package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 
 * BitbucketChangeset
 * 
 * <pre>
 *       {
 *           "node": "21a24da68710",
 *           "files": [
 *               {
 *                   "type": "modified",
 *                   "file": "piston/oauth.py"
 *               }
 *           ],
 *           "author": "jespern",
 *           "timestamp": "2009-09-08 12:49:43",
 *           "branch": "default",
 *           "message": "oauth 1.0a spec ready oauth.py",
 *           "revision": 204,
 *           "size": 4166
 *       }, ...
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
public class BitbucketChangeset implements Serializable
{

	private static final long serialVersionUID = -1593016501516234658L;

	private String node;
	
	private List<BitbucketChangesetFile> files;
	
	private String author;
	
	private Date timestamp;
	
	private String branch;
	
	private String message;
	
	private Integer revision;
	
	private Long size;

	public BitbucketChangeset()
	{
		super();
	}

	public String getNode()
	{
		return node;
	}

	public void setNode(String node)
	{
		this.node = node;
	}

	public List<BitbucketChangesetFile> getFiles()
	{
		return files;
	}

	public void setFiles(List<BitbucketChangesetFile> files)
	{
		this.files = files;
	}

	public String getAuthor()
	{
		return author;
	}

	public void setAuthor(String author)
	{
		this.author = author;
	}

	public Date getTimestamp()
	{
		return timestamp;
	}

	public void setTimestamp(Date timestamp)
	{
		this.timestamp = timestamp;
	}

	public String getBranch()
	{
		return branch;
	}

	public void setBranch(String branch)
	{
		this.branch = branch;
	}

	public String getMessage()
	{
		return message;
	}

	public void setMessage(String message)
	{
		this.message = message;
	}

	public Integer getRevision()
	{
		return revision;
	}

	public void setRevision(Integer revision)
	{
		this.revision = revision;
	}

	public Long getSize()
	{
		return size;
	}

	public void setSize(Long size)
	{
		this.size = size;
	}
}

