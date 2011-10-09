package com.atlassian.jira.plugins.bitbucket.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.google.common.collect.ImmutableList;

public class AggregatedIterator<T> implements Iterator<T>
{
	private final Iterator<Iterator<T>> iterators;
	private Iterator<T> currentIterator = null;

	public AggregatedIterator(Iterator<T>... iterators)
	{
		this.iterators = ImmutableList.of(iterators).iterator();
		if (this.iterators.hasNext())
		{
			this.currentIterator = this.iterators.next();
		}
	}

	public boolean hasNext()
	{
		if (currentIterator == null)
		{
			return false;
		}
		if (currentIterator.hasNext())
		{
			return true;
		}
		if (iterators.hasNext())
		{
			currentIterator = iterators.next();
		}
		return currentIterator.hasNext();
	}

	public T next()
	{
		if (currentIterator == null)
		{
			throw new NoSuchElementException();
		}
		return currentIterator.next();
	}

	public void remove()
	{
		throw new UnsupportedOperationException();
	}
}
