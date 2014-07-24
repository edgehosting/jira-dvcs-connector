package com.atlassian.jira.plugins.dvcs.service.remote;

import com.atlassian.jira.config.FeatureManager;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * Helper class to have all synchronization features in one place
 */
@Component
public class SyncDisabledHelper
{
    public static final String DISABLE_SYNCHRONIZATION_FEATURE = "dvcs.connector.synchronization.disabled";
    public static final String DISABLE_BITBUCKET_SYNCHRONIZATION_FEATURE = "dvcs.connector.synchronization.disabled.bitbucket";
    public static final String DISABLE_GITHUB_SYNCHRONIZATION_FEATURE = "dvcs.connector.synchronization.disabled.github";
    public static final String DISABLE_GITHUB_ENTERPRISE_SYNCHRONIZATION_FEATURE = "dvcs.connector.synchronization.disabled.githube";
    private static final String DISABLE_FULL_SYNCHRONIZATION_FEATURE = "dvcs.connector.full-synchronization.disabled";
    private static final String DISABLE_PR_SYNCHRONIZATION_FEATURE = "dvcs.connector.pr-synchronization.disabled";
    private static final String DISABLE_GITHUB_USE_PR_LIST_FEATURE = "dvcs.connector.pr-synchronization.github.use-pr-list.disabled";

    private final String COMMITS_FALLBACK_FEATURE = "dvcs.connector.pr-synchronization.commits.fallback";

    @Resource
    private FeatureManager featureManager;

    public boolean isBitbucketSyncDisabled()
    {
        return featureManager.isEnabled(DISABLE_SYNCHRONIZATION_FEATURE) || featureManager.isEnabled(DISABLE_BITBUCKET_SYNCHRONIZATION_FEATURE);
    }

    public boolean isGithubSyncDisabled()
    {
        return featureManager.isEnabled(DISABLE_SYNCHRONIZATION_FEATURE) || featureManager.isEnabled(DISABLE_GITHUB_SYNCHRONIZATION_FEATURE);
    }

    public boolean isGithubEnterpriseSyncDisabled()
    {
        return featureManager.isEnabled(DISABLE_SYNCHRONIZATION_FEATURE) || featureManager.isEnabled(DISABLE_GITHUB_ENTERPRISE_SYNCHRONIZATION_FEATURE);
    }

    public boolean isFullSychronizationDisabled()
    {
        return featureManager.isEnabled(DISABLE_FULL_SYNCHRONIZATION_FEATURE);
    }

    public boolean isPullRequestSynchronizationDisabled()
    {
        return featureManager.isEnabled(DISABLE_PR_SYNCHRONIZATION_FEATURE);
    }

    public boolean isSyncDisabled()
    {
        return featureManager.isEnabled(DISABLE_SYNCHRONIZATION_FEATURE);
    }

    public boolean isGitHubUsePullRequestListDisabled()
    {
        return featureManager.isEnabled(DISABLE_GITHUB_USE_PR_LIST_FEATURE);
    }

    public boolean isPullRequestCommitsFallback()
    {
        return featureManager.isEnabled(COMMITS_FALLBACK_FEATURE);
    }
}
