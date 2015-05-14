package com.atlassian.jira.plugins.dvcs;

import com.atlassian.jira.plugins.dvcs.model.Progress;

public class ProgressUtil
{
    public static Progress setErrorMessage(Progress progress, final String errorTitle, final String error, final boolean warning)
    {
        progress.setErrorTitle(errorTitle);
        progress.setError(error);
        progress.setWarning(warning);
        return progress;
    }
}
