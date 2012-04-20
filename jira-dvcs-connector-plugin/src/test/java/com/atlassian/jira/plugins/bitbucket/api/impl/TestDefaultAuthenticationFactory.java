package com.atlassian.jira.plugins.bitbucket.api.impl;

import static junit.framework.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.atlassian.jira.plugins.bitbucket.api.Authentication;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;

public class TestDefaultAuthenticationFactory
{
    @Mock
    private SourceControlRepository repository;

    @Before
    public void setup() throws Exception
    {
        MockitoAnnotations.initMocks(this);
    }
    
    @Test
    public void anonymous()
    {
        DefaultAuthenticationFactory authenticationFactory = new DefaultAuthenticationFactory();
        Authentication authentication = authenticationFactory.getAuthentication(repository);
        assertEquals(Authentication.ANONYMOUS,authentication);
        
    }
    
    @Test
    public void oauth()
    {
        when(repository.getAdminUsername()).thenReturn("");
        when(repository.getAdminPassword()).thenReturn("");
        when(repository.getAccessToken()).thenReturn("abc");
        DefaultAuthenticationFactory authenticationFactory = new DefaultAuthenticationFactory();
        Authentication authentication = authenticationFactory.getAuthentication(repository);
        assertTrue(authentication instanceof GithubOAuthAuthentication);
    }
    
    @Test
    public void basic()
    {
        when(repository.getAdminUsername()).thenReturn("abc");
        when(repository.getAdminPassword()).thenReturn("");
        when(repository.getAccessToken()).thenReturn("");
        DefaultAuthenticationFactory authenticationFactory = new DefaultAuthenticationFactory();
        Authentication authentication = authenticationFactory.getAuthentication(repository);
        assertTrue(authentication instanceof BasicAuthentication);
    }
}
