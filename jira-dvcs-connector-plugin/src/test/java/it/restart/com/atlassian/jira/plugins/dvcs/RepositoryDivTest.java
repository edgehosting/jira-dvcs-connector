package it.restart.com.atlassian.jira.plugins.dvcs;

import com.atlassian.jira.plugins.dvcs.pageobjects.component.RepositoryDiv;
import org.testng.annotations.Test;

import static org.fest.assertions.api.Assertions.assertThat;

public class RepositoryDivTest
{

    RepositoryDiv div = new RepositoryDiv(null);

    @Test
    public void testParseMultipleDash()
    {
        String result = div.parseRepositoryId("sdklfj-lksdjf-1");
        assertThat(result).isEqualTo("1");
    }

    @Test
    public void testParseNoDash()
    {
        String result = div.parseRepositoryId("1");
        assertThat(result).isEqualTo("1");
    }
}