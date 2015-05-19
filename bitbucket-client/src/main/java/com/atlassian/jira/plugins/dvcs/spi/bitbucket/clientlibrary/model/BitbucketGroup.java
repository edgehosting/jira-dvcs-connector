package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;
import java.util.List;

/**
 * BitbucketGroup
 * 
 * <pre>
 * {
 *       "name": "developers",
 *       "permission": "read",
 *       "auto_add": false,
 *       "members": [
 *           {
 *               "username": "jstepka",
 *               "first_name": "Justen",
 *               "last_name": "Stepka",
 *               "avatar": "https://secure.gravatar.com/avatar/12e5043280f67465b68ac42985082498?d=identicon&amp;s=32",
 *               "resource_uri": "/1.0/users/jstepka"
 *           },
 *           {
 *               "username": "detkin",
 *               "first_name": "Dylan",
 *               "last_name": "Etkin",
 *               "avatar": "https://secure.gravatar.com/avatar/e1ef8ef737e394a17ffbc27c889c2b22?d=identicon&amp;s=32",
 *               "resource_uri": "/1.0/users/detkin"
 *           }
 *       ],
 *       "owner": {
 *           "username": "baratrion",
 *           "first_name": "Mehmet S",
 *           "last_name": "Catalbas",
 *           "avatar": "https://secure.gravatar.com/avatar/55a1369161d3a648729b59cabf160e70?d=identicon&amp;s=32",
 *           "resource_uri": "/1.0/users/baratrion"
 *       },
 *       "slug": "developers"
 *   }
 * </pre>
 *
 * @author jhocman@atlassian.com
 */
public class BitbucketGroup implements Serializable
{
	private static final long serialVersionUID = 2971314710849466929L;

	private String name;
	private String perission;
	private Boolean autoAdd;
	private List<BitbucketAccount> members;
	private BitbucketAccount owner;
	private String slug;

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getPerission()
	{
		return perission;
	}

	public void setPerission(String perission)
	{
		this.perission = perission;
	}

	public List<BitbucketAccount> getMembers()
	{
		return members;
	}

	public void setMembers(List<BitbucketAccount> members)
	{
		this.members = members;
	}

	public BitbucketAccount getOwner()
	{
		return owner;
	}

	public void setOwner(BitbucketAccount owner)
	{
		this.owner = owner;
	}

	public String getSlug()
	{
		return slug;
	}

	public void setSlug(String slug)
	{
		this.slug = slug;
	}

	public Boolean isAutoAdd()
	{
		return autoAdd;
	}

	public void setAutoAdd(Boolean autoAdd)
	{
		this.autoAdd = autoAdd;
	}
}
