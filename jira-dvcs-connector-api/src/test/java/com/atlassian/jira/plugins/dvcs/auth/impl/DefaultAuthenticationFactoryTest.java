package com.atlassian.jira.plugins.dvcs.auth.impl;

import com.atlassian.jira.plugins.dvcs.auth.Authentication;
import com.atlassian.jira.plugins.dvcs.auth.AuthenticationFactory;
import com.atlassian.jira.plugins.dvcs.crypto.Encryptor;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.fest.assertions.api.Assertions.*;

/**
 * @author Martin Skurla
 */
public final class DefaultAuthenticationFactoryTest {

    private Repository repositoryMock;
    private Credential credentialMock;
    private Encryptor  encryptorMock;

    @Before
    public void initializeMocks()
    {
        repositoryMock = createStrictMock(Repository.class);
        credentialMock = createStrictMock(Credential.class);
        encryptorMock  = createStrictMock(Encryptor.class);
    }

    @After
    public void verifyMocks()
    {
        verify(repositoryMock, credentialMock, encryptorMock);
    }


    @Test
    public void repositoryWithNullAccessTokenAndAdminUserName_ShouldReturnAnonymousAuthentication()
    {
        expect(repositoryMock.getCredential())   .andReturn(credentialMock);
        expect(credentialMock.getAccessToken())  .andReturn(null);
        expect(credentialMock.getAdminUsername()).andReturn(null);

        replay(repositoryMock, credentialMock, encryptorMock);

        AuthenticationFactory authenticationFactory = new DefaultAuthenticationFactory(null); // no encryptor needed
        Authentication authentication = authenticationFactory.getAuthentication(repositoryMock);

        assertThat(authentication).isEqualTo(Authentication.ANONYMOUS);
    }

    @Test
    public void repositoryWithBlankAccessTokenAndAdminUserName_ShouldReturnAnonymousAuthentication()
    {
        expect(repositoryMock.getCredential())   .andReturn(credentialMock);
        expect(credentialMock.getAccessToken())  .andReturn("");
        expect(credentialMock.getAdminUsername()).andReturn("");

        replay(repositoryMock, credentialMock, encryptorMock);

        AuthenticationFactory authenticationFactory = new DefaultAuthenticationFactory(null); // no encryptor needed
        Authentication authentication = authenticationFactory.getAuthentication(repositoryMock);

        assertThat(authentication).isEqualTo(Authentication.ANONYMOUS);
    }

    @Test
    public void repositoryWithNotNullAccessToken_ShouldReturnOAuthAuthentication()
    {
        expect(repositoryMock.getCredential()).andReturn(credentialMock);

        expect(credentialMock.getAccessToken())  .andReturn("accessToken").anyTimes();
        expect(credentialMock.getAdminUsername()).andReturn(null)         .anyTimes();

        replay(repositoryMock, credentialMock, encryptorMock);

        AuthenticationFactory authenticationFactory = new DefaultAuthenticationFactory(null); // no encryptor needed
        Authentication authentication = authenticationFactory.getAuthentication(repositoryMock);

        assertThat(authentication).isInstanceOf(OAuthAuthentication.class);
    }

    @Test
    public void repositoryWithNotNullAdminUserName_ShouldReturnBasicAuthentication()
    {
        expect(repositoryMock.getCredential()).andReturn(credentialMock);
        expect(repositoryMock.getOrgName())   .andReturn("orgName");
        expect(repositoryMock.getOrgHostUrl()).andReturn("orgHostUrl");

        expect(credentialMock.getAccessToken())  .andReturn(null)           .anyTimes();
        expect(credentialMock.getAdminUsername()).andReturn("admimUserName").anyTimes();
        expect(credentialMock.getAdminPassword()).andReturn("adminPassword").anyTimes();

        expect(encryptorMock.decrypt(anyString(), anyString(), anyString())).andReturn("decryptedStuff");

        replay(repositoryMock, credentialMock, encryptorMock);

        AuthenticationFactory authenticationFactory = new DefaultAuthenticationFactory(encryptorMock);
        Authentication authentication = authenticationFactory.getAuthentication(repositoryMock);

        assertThat(authentication).isInstanceOf(BasicAuthentication.class);
    }

    private static String anyString()
    {
        return EasyMock.anyObject(String.class);
    }
}
