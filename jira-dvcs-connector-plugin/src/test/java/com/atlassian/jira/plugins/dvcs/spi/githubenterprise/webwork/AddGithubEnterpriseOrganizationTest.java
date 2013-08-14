package com.atlassian.jira.plugins.dvcs.spi.githubenterprise.webwork;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.junit.rules.AvailableInContainer;
import com.atlassian.jira.plugins.dvcs.analytics.DvcsConfigAddEndedAnalyticsEvent;
import com.atlassian.jira.plugins.dvcs.analytics.DvcsConfigAddStartedAnalyticsEvent;
import com.atlassian.jira.plugins.dvcs.auth.OAuthStore;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.spi.github.webwork.GithubOAuthUtils;
import com.atlassian.jira.plugins.dvcs.util.TestNGMockComponentContainer;
import com.atlassian.jira.plugins.dvcs.util.TestNGMockHttp;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.sal.api.ApplicationProperties;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import webwork.action.Action;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static com.atlassian.jira.plugins.dvcs.analytics.DvcsConfigAddEndedAnalyticsEvent.*;
import static com.atlassian.jira.plugins.dvcs.spi.githubenterprise.webwork.AddGithubEnterpriseOrganization.EVENT_TYPE_GITHUB_ENTERPRISE;
import static com.atlassian.jira.plugins.dvcs.webwork.CommonDvcsConfigurationAction.DEFAULT_SOURCE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class AddGithubEnterpriseOrganizationTest {
    private static final String SAMPLE_SOURCE = "src";
    private static final String SAMPLE_XSRF_TOKEN = "xsrfToken";
    private static final String SAMPLE_AUTH_URL = "http://authurl.com";

    private final TestNGMockComponentContainer mockComponentContainer = new TestNGMockComponentContainer(this);

    @Mock
    @AvailableInContainer
    private com.atlassian.jira.security.xsrf.XsrfTokenGenerator xsrfTokenGenerator;

    @Mock
    @AvailableInContainer
    private com.atlassian.jira.security.JiraAuthenticationContext jiraAuthenticationContext;

    @Mock
    private I18nHelper i18nHelper;

    @Mock
    @AvailableInContainer
    private com.atlassian.jira.web.action.RedirectSanitiser redirectSanitiser;

    @Mock
    @AvailableInContainer
    private com.atlassian.jira.config.properties.ApplicationProperties jiraApplicationProperties;

    private final TestNGMockHttp mockHttp = TestNGMockHttp.withMockitoMocks();
    private HttpServletRequest request;
    private HttpServletResponse response;

    @Mock
    private ApplicationProperties ap;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private OAuthStore oAuthStore;

    @Mock
    private GithubOAuthUtils githubOAuthUtils;

    private AddGithubEnterpriseOrganization addGithubEnterpriseOrganization;

    @BeforeMethod(alwaysRun=true)
    public void setup()
    {
        MockitoAnnotations.initMocks(this);

        mockComponentContainer.beforeMethod();
        mockHttp.beforeMethod();
        request = mockHttp.mockRequest();
        response = mockHttp.mockResponse();

        when(xsrfTokenGenerator.generateToken(request)).thenReturn(SAMPLE_XSRF_TOKEN);
        when(jiraAuthenticationContext.getI18nHelper()).thenReturn(i18nHelper);
        when(redirectSanitiser.makeSafeRedirectUrl(anyString())).then(returnsFirstArg()); // returns the same url
        when(jiraApplicationProperties.getEncoding()).thenReturn("UTF-8");

        when(oAuthStore.getClientId(anyString())).thenReturn("apiKey");
        when(oAuthStore.getSecret(anyString())).thenReturn("apiSecret");

        when(request.getParameter("oauth_verifier")).thenReturn("verifier");
        HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);
//        Token requestToken = mock(Token.class);
//        when(session.getAttribute(AddBitbucketOrganization.SESSION_KEY_REQUEST_TOKEN)).thenReturn(requestToken);
//        when(requestToken.getToken()).thenReturn("requestToken");
//        when(requestToken.getSecret()).thenReturn("secret");

//        final Token accessToken = mock(Token.class);
//        when(accessToken.getToken()).thenReturn("accessToken");
//        when(accessToken.getSecret()).thenReturn("accessSecret");
//        when(oAuthService.getAccessToken(eq(requestToken), any(Verifier.class))).thenReturn(accessToken);
//        when(oAuthService.getRequestToken()).thenReturn(requestToken);
        when(githubOAuthUtils.createGithubRedirectUrl(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(SAMPLE_AUTH_URL);

        addGithubEnterpriseOrganization = new AddGithubEnterpriseOrganization(ap, eventPublisher, oAuthStore, organizationService)
        {
            @Override
            GithubOAuthUtils getGithubOAuthUtils() {
                return githubOAuthUtils;
            }
        };
        addGithubEnterpriseOrganization.setUrl("http://url.com");
    }

    @AfterMethod
    public void tearDown()
    {
        ComponentAccessor.initialiseWorker(null); // reset
        mockComponentContainer.afterMethod();
        mockHttp.afterMethod();
    }

    @Test
    public void testDoExecuteAnalytics() throws Exception
    {
        addGithubEnterpriseOrganization.setSource(SAMPLE_SOURCE);
        String ret = addGithubEnterpriseOrganization.doExecute();
        assertThat(ret, equalTo(Action.NONE));
        verify(eventPublisher).publish(new DvcsConfigAddStartedAnalyticsEvent(SAMPLE_SOURCE, EVENT_TYPE_GITHUB_ENTERPRISE));
        verifyNoMoreInteractions(eventPublisher);
//        verify(response).sendRedirect(eq(SAMPLE_AUTH_URL + "&t=2)); ??
//        verifyNoMoreInteractions(response);
    }
    @Test
    public void testDoExecuteAnalyticsDefaultSource() throws Exception
    {
        addGithubEnterpriseOrganization.setSource(null);
        String ret = addGithubEnterpriseOrganization.doExecute();
        assertThat(ret, equalTo(Action.NONE));
        verify(eventPublisher).publish(new DvcsConfigAddStartedAnalyticsEvent(DEFAULT_SOURCE, EVENT_TYPE_GITHUB_ENTERPRISE));
        verifyNoMoreInteractions(eventPublisher);
//        verify(response).sendRedirect(eq(SAMPLE_AUTH_URL));
//        verifyNoMoreInteractions(response);
    }

    @Test
    public void testDoFinishAnalytics() throws Exception
    {
        addGithubEnterpriseOrganization.setSource(SAMPLE_SOURCE);
        String ret = addGithubEnterpriseOrganization.doFinish();
        assertThat(ret, equalTo(Action.NONE));
        verify(eventPublisher).publish(new DvcsConfigAddEndedAnalyticsEvent(SAMPLE_SOURCE, EVENT_TYPE_GITHUB_ENTERPRISE, OUTCOME_SUCCEEDED, null));
        verifyNoMoreInteractions(eventPublisher);
        verify(response).sendRedirect(eq("ConfigureDvcsOrganizations.jspa?atl_token=" + SAMPLE_XSRF_TOKEN + "&source=" + SAMPLE_SOURCE));
        verifyNoMoreInteractions(response);
    }

    @Test
    public void testDoFinishAnalyticsDefaultSource() throws Exception
    {
        addGithubEnterpriseOrganization.setSource(null);
        String ret = addGithubEnterpriseOrganization.doFinish();
        assertThat(ret, equalTo(Action.NONE));
        verify(eventPublisher).publish(new DvcsConfigAddEndedAnalyticsEvent(DEFAULT_SOURCE, EVENT_TYPE_GITHUB_ENTERPRISE, OUTCOME_SUCCEEDED, null));
        verifyNoMoreInteractions(eventPublisher);
        verify(response).sendRedirect(eq("ConfigureDvcsOrganizations.jspa?atl_token=" + SAMPLE_XSRF_TOKEN)); // source parameter skipped
        verifyNoMoreInteractions(response);
    }

    @Test
    public void testDoFinishAnalyticsErrorGeneric() throws Exception
    {
        addGithubEnterpriseOrganization.setSource(SAMPLE_SOURCE);
        when(organizationService.save(any(Organization.class))).thenThrow(Exception.class);
        String ret = addGithubEnterpriseOrganization.doFinish();
        assertThat(ret, equalTo(Action.INPUT));
        verify(eventPublisher).publish(new DvcsConfigAddEndedAnalyticsEvent(SAMPLE_SOURCE, EVENT_TYPE_GITHUB_ENTERPRISE, OUTCOME_FAILED, FAILED_REASON_OAUTH_GENERIC));
        verifyNoMoreInteractions(eventPublisher);
        verifyNoMoreInteractions(response);
    }

    @Test
    public void testDoFinishAnalyticsErrorSourceControl() throws Exception
    {
        addGithubEnterpriseOrganization.setSource(SAMPLE_SOURCE);
        reset(organizationService);
        when(organizationService.save(any(Organization.class))).thenThrow(SourceControlException.class);
        String ret = addGithubEnterpriseOrganization.doFinish();
        assertThat(ret, equalTo(Action.INPUT));
        verify(eventPublisher).publish(new DvcsConfigAddEndedAnalyticsEvent(SAMPLE_SOURCE, EVENT_TYPE_GITHUB_ENTERPRISE, OUTCOME_FAILED, FAILED_REASON_OAUTH_SOURCECONTROL));
        verifyNoMoreInteractions(eventPublisher);
        verifyNoMoreInteractions(response);
    }

    @Test
    public void testDoValidationAnalyticsError() throws Exception
    {
        addGithubEnterpriseOrganization.setSource(SAMPLE_SOURCE);
        addGithubEnterpriseOrganization.setOrganization(null); // cause validation error
        addGithubEnterpriseOrganization.doValidation();
        verify(eventPublisher).publish(new DvcsConfigAddEndedAnalyticsEvent(SAMPLE_SOURCE, EVENT_TYPE_GITHUB_ENTERPRISE, OUTCOME_FAILED, FAILED_REASON_VALIDATION));
        verifyNoMoreInteractions(eventPublisher);
    }

    @Test
    public void testDoValidationAnalyticsNoError() throws Exception
    {
        addGithubEnterpriseOrganization.setSource(SAMPLE_SOURCE);
        addGithubEnterpriseOrganization.setOrganization("org");
        addGithubEnterpriseOrganization.doValidation();
        verifyNoMoreInteractions(eventPublisher);
    }
}