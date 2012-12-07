package com.atlassian.jira.plugins.dvcs.util;

import org.testng.SkipException;

/**
 * @author Martin Skurla
 */
public class TestNGAssumptions
{
    private TestNGAssumptions() {}


    public static void assumeThat(boolean expression)
    {
        if (!expression)
        {
            throw new SkipException("Test skipped because assumption was evaluated to false.");
        }
    }
}
