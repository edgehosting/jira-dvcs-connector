package it.restart.com.atlassian.jira.plugins.dvcs.test.matchers;

import it.restart.com.atlassian.jira.plugins.dvcs.OrganizationDiv;
import it.restart.com.atlassian.jira.plugins.dvcs.RepositoryDiv;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.TypeSafeMatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of a Hamcrest matcher that compares a Organization by looking for a specific repository and checking
 * its last message.
 */
public class OrganizationRepositoryMessageMatcher extends TypeSafeMatcher<OrganizationDiv>
{
    private final String expectedRepository;
    private final String expectedMessage;
    private boolean repositoryFound;
    private String actualMessage;
    private List<String> actualRepositories = new ArrayList<String>();

    public OrganizationRepositoryMessageMatcher(final String expectedRepository, final String expectedMessage)
    {
        this.expectedRepository = expectedRepository;
        this.expectedMessage = expectedMessage;
    }

    @Override
    protected boolean matchesSafely(final OrganizationDiv organization)
    {
        List<RepositoryDiv> repositories = organization.getRepositories(true);
        for (RepositoryDiv repositoryDiv : repositories)
        {
            if (expectedRepository.equals(repositoryDiv.getRepositoryName()))
            {
                repositoryFound = true;
                final String message = repositoryDiv.getMessage();
                if (message.equals(expectedMessage))
                {
                    return true;
                }
                else
                {
                    actualMessage = message;
                    return false;
                }
            }
            actualRepositories.add(repositoryDiv.getRepositoryName());
        }

        repositoryFound = false;
        return false;
    }

    @Override
    public void describeTo(final Description description)
    {
        if (repositoryFound)
        {
            description.appendText("expected message " + expectedMessage + " but found " + actualMessage);
        }
        else
        {
            description.appendText("expected to find repository " + expectedRepository + " but did not find a respository with that name, instead ");
            description.appendValueList("{", ",", "}", actualRepositories);
        }
    }

    @Factory
    public static OrganizationRepositoryMessageMatcher expectedRepositoryMatcher(String repositoryName, String message)
    {
        return new OrganizationRepositoryMessageMatcher(repositoryName, message);
    }
}
