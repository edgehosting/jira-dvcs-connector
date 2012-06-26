package com.atlassian.jira.plugins.dvcs.auth.impl;

import com.atlassian.jira.plugins.dvcs.auth.Authentication;
import com.atlassian.jira.plugins.dvcs.auth.AuthenticationFactory;
import com.atlassian.jira.plugins.dvcs.crypto.Encryptor;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.fest.assertions.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Martin Skurla
 */
@RunWith(MockitoJUnitRunner.class)
public final class DefaultAuthenticationFactoryTest {

    @Mock
    private Repository repositoryMock;

    @Mock
    private Credential credentialMock;

    @Mock
    private Encryptor  encryptorMock;


    @Test
    public void repositoryWithNullAccessTokenAndAdminUserName_ShouldReturnAnonymousAuthentication()
    {
        when(repositoryMock.getCredential())   .thenReturn(credentialMock);
        when(credentialMock.getAccessToken())  .thenReturn(null); // not necessary, but null value has to be handled
        when(credentialMock.getAdminUsername()).thenReturn(null); // not necessary, but null value has to be handled

        AuthenticationFactory authenticationFactory = new DefaultAuthenticationFactory(null); // no encryptor needed
        Authentication authentication = authenticationFactory.getAuthentication(repositoryMock);

        assertThat(authentication).isEqualTo(Authentication.ANONYMOUS);
    }

    @Test
    public void repositoryWithBlankAccessTokenAndAdminUserName_ShouldReturnAnonymousAuthentication()
    {
        when(repositoryMock.getCredential())   .thenReturn(credentialMock);
        when(credentialMock.getAccessToken())  .thenReturn("");
        when(credentialMock.getAdminUsername()).thenReturn("");

        AuthenticationFactory authenticationFactory = new DefaultAuthenticationFactory(null); // no encryptor needed
        Authentication authentication = authenticationFactory.getAuthentication(repositoryMock);

        assertThat(authentication).isEqualTo(Authentication.ANONYMOUS);
    }

    @Test
    public void repositoryWithNotNullAccessToken_ShouldReturnOAuthAuthentication()
    {
        when(repositoryMock.getCredential())   .thenReturn(credentialMock);
        when(credentialMock.getAccessToken())  .thenReturn("accessToken");
        when(credentialMock.getAdminUsername()).thenReturn(null);

        AuthenticationFactory authenticationFactory = new DefaultAuthenticationFactory(null); // no encryptor needed
        Authentication authentication = authenticationFactory.getAuthentication(repositoryMock);

        assertThat(authentication).isInstanceOf(OAuthAuthentication.class);
    }

    @Test
    public void repositoryWithNotNullAdminUserName_ShouldReturnBasicAuthentication()
    {
        when(repositoryMock.getCredential()).thenReturn(credentialMock);
        when(repositoryMock.getOrgName())   .thenReturn("orgName");
        when(repositoryMock.getOrgHostUrl()).thenReturn("orgHostUrl");

        when(credentialMock.getAccessToken())  .thenReturn(null);
        when(credentialMock.getAdminUsername()).thenReturn("admimUserName");
        when(credentialMock.getAdminPassword()).thenReturn("adminPassword");

        AuthenticationFactory authenticationFactory = new DefaultAuthenticationFactory(encryptorMock);
        Authentication authentication = authenticationFactory.getAuthentication(repositoryMock);

        assertThat(authentication).isInstanceOf(BasicAuthentication.class);
    }
}