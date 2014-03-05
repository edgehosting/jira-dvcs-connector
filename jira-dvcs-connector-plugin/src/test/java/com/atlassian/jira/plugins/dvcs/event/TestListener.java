package com.atlassian.jira.plugins.dvcs.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Base class for testing event dispatching.
 */
public class TestListener<T>
{
    List<T> created = new CopyOnWriteArrayList<T>();
}
