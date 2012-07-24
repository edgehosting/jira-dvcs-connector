package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;
import java.util.List;

/**
 * 
 * BitbucketChangesetEnvelope
 * 
 * <pre>
 *   "count": 219,
 *   "start": "tip",
 *   "limit": 15,
 *   "changesets": [
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
public class BitbucketChangesetEnvelope implements Serializable
{
	private static final long serialVersionUID = 2124466515770630375L;
	
	private Integer count;
	private String start;
	private Integer limit;
	private List<BitbucketChangeset> changesets;

	public Integer getCount()
	{
		return count;
	}

	public void setCount(Integer count)
	{
		this.count = count;
	}

	public String getStart()
	{
		return start;
	}

	public void setStart(String start)
	{
		this.start = start;
	}

	public Integer getLimit()
	{
		return limit;
	}

	public void setLimit(Integer limit)
	{
		this.limit = limit;
	}

	public List<BitbucketChangeset> getChangesets()
	{
		return changesets;
	}

	public void setChangesets(List<BitbucketChangeset> changesets)
	{
		this.changesets = changesets;
	}
}
