package com.atlassian.jira.plugins.dvcs.base;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;

/**
 * Listener for test lifecycle.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public interface TestListener
{

    /**
     * @see BeforeMethod
     */
    void beforeMethod();

    /**
     * @see AfterMethod
     */
    void afterMethod();

    /**
     * @see AfterTest
     */
    void afterClass();

}
