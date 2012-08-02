package com.atlassian.jira.plugins.dvcs.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultPluginFeatureDetector implements IPluginFeatureDetector
{
    
    private static final Logger log = LoggerFactory.getLogger(DefaultPluginFeatureDetector.class);

    @Override
    public boolean isUserInvitationsEnabled()
    {
        try
        {
            Class.forName("com.atlassian.jira.event.web.action.admin.UserAddedEvent");
            
            return true;
            
        } catch (ClassNotFoundException cnfe) {
            
            log.debug("UserAddedEvent not available: " + cnfe.getMessage());
            
        }
        
        return false;
    }

}

