package it.restart.com.atlassian.jira.plugins.dvcs.test.matchers;

import com.atlassian.pageobjects.elements.PageElement;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import it.restart.com.atlassian.jira.plugins.dvcs.OrganizationDiv;
import it.restart.com.atlassian.jira.plugins.dvcs.RepositoryDiv;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class OrganizationRepositoryMatcherTest
{
    private static final String[] BASE_REPOSITORY_NAMES = new String[] { "private-git-repo", "private-hg-repo", "public-git-repo", "public-hg-repo" };

    @Test
    public void testMatchesWhenExtraRepo()
    {
        String[] actualRepositories = new String[] { "breaks integration test2", "private-git-repo", "private-hg-repo", "public-git-repo", "public-hg-repo" };

        OrganizationDiv div = new OrganizationDivForTesting(Arrays.asList(actualRepositories));

        assertThat(div, OrganizationRepositoryMatcher.expectedRepositoryMatcher(BASE_REPOSITORY_NAMES));
    }

    @Test
    public void testFailsWhenMissingRepo()
    {
        String[] actualRepositories = new String[] { "breaks integration test2", "private-hg-repo", "public-git-repo", "public-hg-repo" };

        OrganizationDiv div = new OrganizationDivForTesting(Arrays.asList(actualRepositories));

        final OrganizationRepositoryMatcher matcher = OrganizationRepositoryMatcher.expectedRepositoryMatcher(BASE_REPOSITORY_NAMES);
        assertThat(matcher.matchesSafely(div), is(false));
    }

    private class OrganizationDivForTesting extends OrganizationDiv
    {

        private final List<String> repositories;

        public OrganizationDivForTesting(List<String> repositories)
        {
            super(Mockito.mock(PageElement.class));
            this.repositories = repositories;
        }

        @Override
        public List<RepositoryDiv> getRepositories(final boolean filterDynamicRepositories)
        {
            return Lists.transform(repositories, new Function<String, RepositoryDiv>()
            {
                @Override
                public RepositoryDiv apply(final String name)
                {
                    return new RepositoryDivForTesting(name);
                }
            });
        }
    }

    private class RepositoryDivForTesting extends RepositoryDiv
    {

        private final String name;

        public RepositoryDivForTesting(String name)
        {
            super(null);
            this.name = name;
        }

        @Override
        public String getRepositoryName()
        {
            return name;
        }
    }
}
