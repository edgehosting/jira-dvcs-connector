package it.com.atlassian.jira.plugins.dvcs;

import org.testng.annotations.Listeners;

/**
 * Base class of all webdriver tests.
 * <p/>
 * It adds screenshot rule that would capture screenshot when a test fails.
 */
// note that adding this annotations applies the listeners to all tests, but that's exactly what we want
@Listeners({WebDriverScreenshotListener.class})
public abstract class DvcsWebDriverTestCase
{
}
