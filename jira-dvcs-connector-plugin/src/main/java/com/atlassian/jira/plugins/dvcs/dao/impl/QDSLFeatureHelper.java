package com.atlassian.jira.plugins.dvcs.dao.impl;

import com.atlassian.jira.config.FeatureManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class contains the feature flags for controlling the various retrieval services and whether they are using QueryDSL
 */
@Component
public class QDSLFeatureHelper
{
    public static final String RETRIEVE_USING_QDSL =  "dvcs.connector.retrieved.using.qdsl";

    @ComponentImport
    private final FeatureManager featureManager;

    @Autowired
    public QDSLFeatureHelper(final FeatureManager featureManager)
    {
        this.featureManager = featureManager;
    }

    public boolean isRetrievalUsingQDSLEnabled()
    {
        return featureManager.isEnabled(RETRIEVE_USING_QDSL);
    }
}
