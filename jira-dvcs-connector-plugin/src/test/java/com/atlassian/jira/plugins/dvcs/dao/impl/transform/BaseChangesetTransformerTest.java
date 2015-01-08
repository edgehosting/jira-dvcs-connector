package com.atlassian.jira.plugins.dvcs.dao.impl.transform;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.ChangesetMapping;
import com.atlassian.jira.plugins.dvcs.dao.impl.ChangesetDaoImpl;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BaseChangesetTransformerTest
{
    @Mock
    protected ActiveObjects activeObjects;

    @Mock
    protected ChangesetDaoImpl changesetDao;

    protected ChangesetTransformer changesetTransformer;

    @BeforeMethod
    public void init()
    {
        MockitoAnnotations.initMocks(this);
        changesetTransformer = new ChangesetTransformer(activeObjects, changesetDao);
    }

    protected ChangesetMapping mockChangesetMapping(int fileCount, boolean withFilesData)
    {
        ChangesetMapping changesetMapping = mock(ChangesetMapping.class);
        if (withFilesData)
        {
            when(changesetMapping.getFileCount()).thenReturn(0);
        }
        else
        {
            when(changesetMapping.getFileCount()).thenReturn(fileCount);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < Math.min(fileCount, Changeset.MAX_VISIBLE_FILES); i++)
        {
            if (i > 0)
            {
                sb.append(",");
            }
            sb.append("{\"fileAction\":\"MODIFIED\",\"file\":\"file").append(i + 1).append("\",\"additions\":1,\"deletions\":1}");
        }
        sb.append("]");
        when(changesetMapping.getFileDetailsJson()).thenReturn(sb.toString());
        if (withFilesData)
        {
            when(changesetMapping.getFilesData()).thenReturn("{\"count\":" + fileCount +  "}");
        }
        when(changesetMapping.getNode()).thenReturn("111111111111111");
        return changesetMapping;
    }
}
