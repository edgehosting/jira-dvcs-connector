package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;
import java.util.List;

/**
 * 
 * BitbucketRepositoryEnvelope
 * <pre>
 * {"repositories":[
 * 
 * {
 *     "slug": "django-piston",
 *     "name": "django-piston",
 *     "resource_uri": "/1.0/repositories/jespern/django-piston/",
 *     "followers_count": 173,
 *     "website": "",
 *     "description": "Piston is a Django mini-framework creating APIs."
 *   } , ...
 * </pre>
 * <br /><br />
 * Created on 12.7.2012, 16:51:23
 * <br /><br />
 * @author jhocman@atlassian.com
 *
 */
public class BitbucketRepositoryEnvelope implements Serializable
{
	private static final long serialVersionUID = -3901290942744823510L;

	private List<BitbucketRepository> repositories;

	public List<BitbucketRepository> getRepositories()
	{
		return repositories;
	}

	public void setRepositories(List<BitbucketRepository> repositories)
	{
		this.repositories = repositories;
	}
}
