package it.com.atlassian.jira.plugins.dvcs;

import com.atlassian.webdriver.testing.rule.WebDriverScreenshotRule;
import org.junit.Rule;

/**
 * Base class of all webdriver tests.
 * <p/>
 * It adds screenshot rule that would capture screenshot when a test fails.
 */
public abstract class DvcsWebDriverTestCase
{
    @Rule
    public WebDriverScreenshotRule screenshotRule = new WebDriverScreenshotRule();
}
