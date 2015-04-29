package it.com.atlassian.jira.plugins.dvcs.cleanup;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.plugins.dvcs.util.PasswordUtil;
import com.atlassian.pageobjects.TestedProductFactory;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.io.IOException;

import static com.atlassian.jira.plugins.dvcs.pageobjects.common.OAuthUtils.TEST_OAUTH_PREFIX;
import static org.apache.commons.lang3.StringUtils.trim;

/**
 * Base class to delete orphan OAUTH Applications (expired applications) for multiple repo owners in Bitbucket and
 * Github.
 * <p/>
 * The orphan applications are created by webdriver tests and leaked when some unexpected failure happens and they have
 * a standard name Test_OAuth_[date/time in milliseconds].
 */
public abstract class DeleteOrphanAppsBaseTest
{
    protected static final Logger log = LoggerFactory.getLogger(DeleteOrphanAppsBaseTest.class);

    protected static final String[] REPO_OWNERS = { "dvcsconnectortest", "jirabitbucketconnector" };

    private static final int CONSUMER_EXPIRY_DAYS = 3;
    private static final DateTime CUT_OFF_DATE = new DateTime().minusDays(CONSUMER_EXPIRY_DAYS);

    protected JiraTestedProduct jira = TestedProductFactory.create(JiraTestedProduct.class);

    @Test
    public void deleteOrphanOAuthApplications()
    {
        for (String owner : REPO_OWNERS)
        {
            try
            {
                final String password = PasswordUtil.getPassword(owner);
                login(owner, password);
                deleteOrphanOAuthApplications(owner, password);
            }
            catch (Exception e)
            {
                log.warn("Failed to delete OAUTH expired consumers", e);
            }
            finally
            {
                logout();
            }
        }
    }

    /**
     * @return true if appName matches Test_OAuth_<datetime in millis> and is expired
     */
    protected boolean isConsumerExpired(final String consumerName)
    {
        try
        {
            final String appName = trim(consumerName);

            // look for orphan consumers created by this test previously: Test_OAuth_<datetime in millis>
            if (appName.startsWith(TEST_OAUTH_PREFIX))
            {
                DateTime createdAt = parseDate(appName.substring(TEST_OAUTH_PREFIX.length()));
                return createdAt.isBefore(CUT_OFF_DATE);
            }
            return false;
        }
        catch (RuntimeException e)
        {
            log.warn("Failure while parsing OAUTH Consumer: " + consumerName, e);
            return false;
        }
    }

    protected abstract void deleteOrphanOAuthApplications(final String repoOwner, final String repoPassword)
            throws IOException;

    protected abstract void login(final String repoOwner, final String repoPassword);

    protected abstract void logout();

    private DateTime parseDate(final String date)
    {
        return new DateTime(Long.parseLong(date));
    }
}
