package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;
import java.util.Iterator;

public class RemoteResultIterator<T> implements Serializable, Iterable<T>, Iterator<T>
{

	private static final long serialVersionUID = 2005776685881268059L;

	public RemoteResultIterator()
	{
		super();
	}

	@Override
	public boolean hasNext()
	{
		return false;
	}

	@Override
	public T next()
	{
		return null;
	}

	@Override
	public void remove()
	{
		
	}

	@Override
	public Iterator<T> iterator()
	{
		return this;
	}
	
	
	
	
}

