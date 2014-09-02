package com.atlassian.jira.plugins.dvcs.service.admin;

public interface AdministrationService
{
    boolean primeDevSummaryCache();

    DevSummaryCachePrimingStatus getPrimingStatus();

    void stopPriming();
}
