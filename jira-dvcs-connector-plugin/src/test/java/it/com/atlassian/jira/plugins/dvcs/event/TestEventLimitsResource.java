package it.com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.jira.functest.framework.FunctTestConstants;
import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.testkit.client.Backdoor;
import com.atlassian.pageobjects.TestedProductFactory;
import com.google.common.collect.ImmutableMap;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Map;

import static com.atlassian.jira.plugins.dvcs.event.EventLimit.BRANCH;
import static com.atlassian.jira.plugins.dvcs.event.EventLimit.COMMIT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class TestEventLimitsResource
{
    private static JiraTestedProduct jira = TestedProductFactory.create(JiraTestedProduct.class);
    private LimitsClient limitsClient;
    protected Backdoor testKit;

    @BeforeMethod
    public void setUp() throws Exception
    {
        testKit = new Backdoor(jira.environmentData());
        limitsClient = new LimitsClient(jira.environmentData());
        testKit.restoreBlankInstance();
    }

    @Test
    public void limitsApiIsOnlyAccessibleToAdmins() throws Exception
    {
        limitsClient.loginAs(FunctTestConstants.FRED_USERNAME);

        assertThat(limitsClient.getLimits().status(), equalTo(403));
        assertThat(limitsClient.setLimits(ImmutableMap.of("some_limit", 1)).status(), equalTo(403));
    }

    @Test
    public void limitsApiAllowsOverridingLimitsViaRest() throws Exception
    {
        LimitsResponse response = limitsClient.getLimits();
        assertThat(response.limits(), equalTo((Map) ImmutableMap.of(
                COMMIT.name(), COMMIT.getDefaultLimit(),
                BRANCH.name(), BRANCH.getDefaultLimit()
        )));

        int newLimit = 15;
        assertThat("branch limits should now be " + newLimit, limitsClient.setLimits(ImmutableMap.of(BRANCH.name(), newLimit)).limits(), equalTo((Map) ImmutableMap.of(
                COMMIT.name(), COMMIT.getDefaultLimit(),
                BRANCH.name(), newLimit
        )));

        assertThat("branch limits override should be removed", limitsClient.setLimits(Collections.<String, Integer>singletonMap(BRANCH.name(), null)).limits(), equalTo((Map) ImmutableMap.of(
                COMMIT.name(), COMMIT.getDefaultLimit(),
                BRANCH.name(), BRANCH.getDefaultLimit()
        )));
    }
}
