package com.atlassian.jira.plugins.dvcs.util;

import com.atlassian.jira.junit.rules.MockComponentContainer;

/**
 * Make use of the existing MockComponentContainer class in jira core to support mocking ComponentContainer in TestNG.
 */
public class TestNGMockComponentContainer extends MockComponentContainer {
    public TestNGMockComponentContainer(Object test) {
        super(test);
    }

    public void beforeMethod()
    {
        starting(null);
    }

    public void afterMethod()
    {
        finished(null);
    }
}
