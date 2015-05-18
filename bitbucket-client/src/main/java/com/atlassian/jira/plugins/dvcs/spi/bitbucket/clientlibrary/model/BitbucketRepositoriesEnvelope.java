package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;
import java.util.List;

/**
 * 
 * BitbucketRepositoriesEnvelope
 *
 * <pre>
 * {
 * "repositories": [
 *   {
 *     "slug": "django-piston",
 *     "name": "django-piston",
 *     "resource_uri": "/1.0/repositories/jespern/django-piston/",
 *     "followers_count": 173,
 *     "website": "",
 *     "description": "Piston is a Django mini-framework creating APIs."
 *   }
 * ],
 * "user": {
 *   "username": "jespern",
 *   "avatar": "https://secure.gravatar.com/avatar/b658715b9635ef057daf2a22d4a8f36e?d=identicon&amp;s=32",
 *   "resource_uri": "/1.0/users/jespern/",
 *   "last_name": "Noehr",
 *   "first_name": "Jesper"
 *  }
 * }
 * </pre>
 */
public class BitbucketRepositoriesEnvelope implements Serializable
{
	private static final long serialVersionUID = 5847138841511042184L;

	private List<BitbucketRepository> repositories;
	private BitbucketAccount user;
	
	public List<BitbucketRepository> getRepositories()
	{
		return repositories;
	}

	public void setRepositories(List<BitbucketRepository> repositories)
	{
		this.repositories = repositories;
	}

	public BitbucketAccount getUser()
	{
		return user;
	}

	public void setUser(BitbucketAccount user)
	{
		this.user = user;
	}
}
