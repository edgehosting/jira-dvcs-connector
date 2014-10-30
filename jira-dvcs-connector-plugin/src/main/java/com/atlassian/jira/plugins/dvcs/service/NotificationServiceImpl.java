package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.jira.plugins.dvcs.event.ThreadEvents;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class NotificationServiceImpl implements NotificationService
{
    @Resource
    private ThreadEvents threadEvents;

    @Override
    public void broadcast(final Object event)
    {
        threadEvents.broadcast(event);
    }
}
