package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 
 * BitbucketChangeset
 * 
 * <pre>
 * {
 *     "node": "abdeaf1b2b4a",
 *     "files": [
 *         {
 *             "type": "added",
 *             "file": "AnotherFile.txt"
 *         },
 *         {
 *             "type": "modified",
 *             "file": "Readme"
 *         }
 *     ],
 *     "raw_author": "Mary Anthony &lt;manthony@172-28-13-105.staff.sf.atlassian.com&gt;",
 *     "utctimestamp": "2012-07-23 22:26:36+00:00",
 *     "author": "Mary Anthony",
 *     "timestamp": "2012-07-24 00:26:36",
 *     "raw_node": "abdeaf1b2b4a6b9ddf742c1e1754236380435a62",
 *     "parents": [
 *         "86432202a2d5"
 *     ],
 *     "branch": "master",
 *     "message": "making some changes\n",
 *     "revision": null,
 *     "size": -1
 * }, ...
 * </pre>
 *
 * @author jhocman@atlassian.com
 */
public class BitbucketChangeset implements Serializable
{
	private static final long serialVersionUID = -1593016501516234658L;

	private String node;
	private List<BitbucketChangesetFile> files;
	private String author;
	private String branch;
	private String message;
	private Integer revision;
	private Long size;
    private String rawNode;
    private String rawAuthor;
    // timestamp parameter is not used because Gson SimpleDateFormat pattern is set to parse utc times only
    private Date utctimestamp;
    private List<String> parents;

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

    public String getRawNode()
    {
        return rawNode;
    }

    public void setRawNode(String rawNode)
    {
        this.rawNode = rawNode;
    }

    public String getRawAuthor()
    {
        return rawAuthor;
    }

    public void setRawAuthor(String rawAuthor)
    {
        this.rawAuthor = rawAuthor;
    }

    public Date getUtctimestamp()
    {
        return utctimestamp;
    }

    public void setUtctimestamp(Date utctimestamp)
    {
        this.utctimestamp = utctimestamp;
    }

    public List<String> getParents()
    {
        return parents;
    }

    public void setParents(List<String> parents)
    {
        this.parents = parents;
    }
}
