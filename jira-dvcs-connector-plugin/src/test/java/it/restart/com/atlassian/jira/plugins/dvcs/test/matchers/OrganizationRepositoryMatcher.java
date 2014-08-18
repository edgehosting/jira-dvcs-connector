package it.restart.com.atlassian.jira.plugins.dvcs.test.matchers;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import it.restart.com.atlassian.jira.plugins.dvcs.OrganizationDiv;
import it.restart.com.atlassian.jira.plugins.dvcs.RepositoryDiv;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.TypeSafeMatcher;

import java.util.Arrays;
import java.util.List;

/**
 * Implementation of a Hamcrest matcher that compares a Organization based on the Repositories it has against an
 * expected set of Repositories, returns true if the supplied Organization contains all the expected Repositories.
 */
public class OrganizationRepositoryMatcher extends TypeSafeMatcher<OrganizationDiv>
{
    private List<String> expectedRepositories;
    private List<String> actualRepositories;

    private OrganizationRepositoryMatcher(String... expectedRepositories)
    {
        this.expectedRepositories = Arrays.asList(expectedRepositories);
    }

    @Override
    protected boolean matchesSafely(final OrganizationDiv organization)
    {
        List<String> repositoryNames = Lists.transform(organization.getRepositories(true), new Function<RepositoryDiv, String>()
        {
            @Override
            public String apply(final RepositoryDiv repositoryDiv)
            {
                return repositoryDiv.getRepositoryName();
            }
        });

        actualRepositories = repositoryNames;

        return actualRepositories.containsAll(expectedRepositories);
    }

    @Override
    public void describeTo(final Description description)
    {
        description.appendText("Expected");
        description.appendValueList("{", ",", "}", expectedRepositories);
        description.appendText("Actual");
        if (actualRepositories == null)
        {
            description.appendText(" was null ");
        }
        else
        {
            description.appendValueList("{", ",", "}", actualRepositories);
        }
    }

    @Factory
    public static OrganizationRepositoryMatcher expectedRepositoryMatcher(String... repositoryNames)
    {
        return new OrganizationRepositoryMatcher(repositoryNames);
    }
}
