package com.atlassian.jira.plugins.dvcs.dao.impl.transform;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryMapping;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class RepositoryTransformerTest
{
    private RepositoryTransformer repositoryTransformer;

    @Mock
    private RepositoryMapping repositoryMapping;

    @BeforeMethod
    public void setup()
    {
        repositoryTransformer = new RepositoryTransformer();
    }

    @Test
    public void testWorksWithNullOrgAndProgress()
    {
        repositoryTransformer.transform(repositoryMapping, null, null);
    }
}
