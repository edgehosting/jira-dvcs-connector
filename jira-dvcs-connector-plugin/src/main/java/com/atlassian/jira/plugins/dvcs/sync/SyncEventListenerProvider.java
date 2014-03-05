package com.atlassian.jira.plugins.dvcs.sync;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.ImmutableList.copyOf;

/**
 * Provider of SyncEventListener instances. This provider class is used to break up circular dependencies.
 */
@Component
public class SyncEventListenerProvider
{
    @Autowired
    private ApplicationContext applicationContext;

    /**
     * @return all SyncEventListener instances
     */
    public List<SyncEventListener> getAll()
    {
        Map beans = BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, SyncEventListener.class);

        //noinspection unchecked
        return copyOf(beans.values());
    }
}
