package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.util.List;

/**
 * Mark class than we can obtain parsable messages where are
 * issue keys we searched for. 
 */
public interface HasMessages
{

    List<String> getMessages();
    
}

