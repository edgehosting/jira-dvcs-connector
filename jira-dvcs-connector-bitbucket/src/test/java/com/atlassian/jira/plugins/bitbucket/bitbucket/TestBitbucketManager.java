package com.atlassian.jira.plugins.bitbucket.bitbucket;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.atlassian.jira.plugins.bitbucket.activeobjects.v2.ProjectMapping;
import com.atlassian.jira.plugins.bitbucket.api.Communicator;
import com.atlassian.jira.plugins.bitbucket.api.Encryptor;
import com.atlassian.jira.plugins.bitbucket.api.RepositoryPersister;
import com.atlassian.jira.plugins.bitbucket.api.RepositoryUri;
import com.atlassian.jira.plugins.bitbucket.spi.impl.BitbucketRepositoryManager;
import com.atlassian.jira.plugins.bitbucket.spi.impl.BitbucketRepositoryUri;

public class TestBitbucketManager
{
    private static final RepositoryUri REPOSITORY_URI = new BitbucketRepositoryUri("https","bitbucket.org","owner","slug");
    private static final String URL = "https://bitbucket.org/owner/slug";
    private static final int SOME_ID = 123;

    @Mock
    private Communicator bitbucket;
    @Mock
    private Encryptor encryptor;
    @Mock
    private RepositoryPersister repositoryPersister;
    @Mock
    private ProjectMapping projectMapping;

    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        when(projectMapping.getID()).thenReturn(SOME_ID);
        when(projectMapping.getRepositoryUrl()).thenReturn(URL);
        when(repositoryPersister.addRepository(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(projectMapping);
        when(encryptor.encrypt(anyString(), anyString(), anyString())).thenAnswer(new Answer<String>()
        {
            @Override
            public String answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                return StringUtils.reverse(String.valueOf(invocationOnMock.getArguments()[0]));
            }
        });
        when(encryptor.decrypt(anyString(), anyString(), anyString())).thenAnswer(new Answer<String>()
        {
            @Override
            public String answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                return StringUtils.reverse(String.valueOf(invocationOnMock.getArguments()[0]));
            }
        });
    }
    
    @Test
    public void testPasswordNotStoredInPlainText()
    {
        // TODO: check test validity after removing username/passwd and using only adminusername/adminpswd
        new BitbucketRepositoryManager(repositoryPersister, bitbucket, encryptor, null, null, null, null)
            .addRepository("bitbucket", "JST", REPOSITORY_URI.getRepositoryUrl(), "user", "pass", "");

        verify(repositoryPersister, times(1)).addRepository(null,"bitbucket","JST","https://bitbucket.org/owner/slug","user","ssap","");
        verify(encryptor, times(1)).encrypt("pass", "JST", "https://bitbucket.org/owner/slug");
    }

}
