package com.atlassian.jira.plugins.dvcs.dao.impl;

import com.atlassian.jira.config.FeatureManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This class contains the feature flags for controlling the various retrieval services and whether they are using
 * QueryDSL
 */
@SuppressWarnings ("SpringJavaAutowiringInspection")
@Component
public class QueryDslFeatureHelper
{
    /**
     * If this dark feature is set then we will use Query DSL to retrieve entities for the Dvcs* services that are
     * exported. For example {@link com.atlassian.jira.plugins.dvcs.service.api.DvcsChangesetService }, {@link
     * com.atlassian.jira.plugins.dvcs.service.api.DvcsPullRequestService} and {@link
     * com.atlassian.jira.plugins.dvcs.service.api.DvcsBranchService}
     */
    private static final String RETRIEVE_USING_QUERY_DSL = "dvcs.connector.retrieved.using.qdsl";

    private final FeatureManager featureManager;

    @Autowired
    public QueryDslFeatureHelper(@ComponentImport final FeatureManager featureManager)
    {
        this.featureManager = checkNotNull(featureManager);
    }

    /**
     * Check the dark feature flag {@link #RETRIEVE_USING_QUERY_DSL}
     *
     * @return true if the dark feature is set
     */
    public boolean isRetrievalUsingQueryDSLEnabled()
    {
        return featureManager.isEnabled(RETRIEVE_USING_QUERY_DSL);
    }
}
