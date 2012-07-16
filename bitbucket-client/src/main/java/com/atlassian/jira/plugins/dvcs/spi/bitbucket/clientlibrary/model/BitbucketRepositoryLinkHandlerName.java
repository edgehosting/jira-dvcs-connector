package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

/**
 * @author Martin Skurla mskurla@atlassian.com
 */
public enum BitbucketRepositoryLinkHandlerName
{
    BAMBOO,
    CRUCIBLE,
    CUSTOM,
    JENKINS,
    JIRA;
    
    
    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
