package com.atlassian.jira.plugins.dvcs.spi.bitbucket.webwork;

import static com.atlassian.jira.plugins.dvcs.analytics.DvcsConfigAddEndedAnalyticsEvent.FAILED_REASON_OAUTH_SOURCECONTROL;
import static com.atlassian.jira.plugins.dvcs.analytics.DvcsConfigAddEndedAnalyticsEvent.FAILED_REASON_OAUTH_TOKEN;
import static com.atlassian.jira.plugins.dvcs.analytics.DvcsConfigAddEndedAnalyticsEvent.FAILED_REASON_OAUTH_UNAUTH;
import static com.atlassian.jira.plugins.dvcs.analytics.DvcsConfigAddEndedAnalyticsEvent.FAILED_REASON_VALIDATION;
import static com.atlassian.jira.plugins.dvcs.analytics.DvcsConfigAddEndedAnalyticsEvent.OUTCOME_FAILED;
import static com.atlassian.jira.plugins.dvcs.analytics.DvcsConfigAddEndedAnalyticsEvent.OUTCOME_SUCCEEDED;
import static com.atlassian.jira.plugins.dvcs.spi.bitbucket.webwork.AddBitbucketOrganization.EVENT_TYPE_BITBUCKET;
import static com.atlassian.jira.plugins.dvcs.webwork.CommonDvcsConfigurationAction.DEFAULT_SOURCE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.HttpClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.webwork.AddBitbucketOrganization;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.scribe.exceptions.OAuthException;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import webwork.action.Action;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.junit.rules.AvailableInContainer;
import com.atlassian.jira.plugins.dvcs.analytics.DvcsConfigAddEndedAnalyticsEvent;
import com.atlassian.jira.plugins.dvcs.analytics.DvcsConfigAddStartedAnalyticsEvent;
import com.atlassian.jira.plugins.dvcs.auth.OAuthStore;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator;
import com.atlassian.jira.plugins.dvcs.util.TestNGMockComponentContainer;
import com.atlassian.jira.plugins.dvcs.util.TestNGMockHttp;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.xsrf.XsrfTokenGenerator;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.web.action.RedirectSanitiser;
import com.atlassian.sal.api.ApplicationProperties;

public class AddBitbucketOrganizationTest
{
    private static final String SAMPLE_SOURCE = "src";
    private static final String SAMPLE_XSRF_TOKEN = "xsrfToken";
    private static final String SAMPLE_AUTH_URL = "http://authurl.com";

    private final TestNGMockComponentContainer mockComponentContainer = new TestNGMockComponentContainer(this);

    @Mock
    @AvailableInContainer
    private XsrfTokenGenerator xsrfTokenGenerator;

    @Mock
    @AvailableInContainer
    private JiraAuthenticationContext jiraAuthenticationContext;

    @Mock
    private I18nHelper i18nHelper;

    @Mock
    @AvailableInContainer
    private RedirectSanitiser redirectSanitiser;

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
    private OAuthService oAuthService;

    @Mock
    private HttpClientProvider httpClientProvider;

    private AddBitbucketOrganization addBitbucketOrganization;

    @BeforeMethod (alwaysRun=true)
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

        when(oAuthStore.getClientId(anyString())).thenReturn("apiKey");
        when(oAuthStore.getSecret(anyString())).thenReturn("apiSecret");

        when(request.getParameter("oauth_verifier")).thenReturn("verifier");
        HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);
        Token requestToken = mock(Token.class);
        when(session.getAttribute(AddBitbucketOrganization.SESSION_KEY_REQUEST_TOKEN)).thenReturn(requestToken);
//        when(requestToken.getToken()).thenReturn("requestToken");
//        when(requestToken.getSecret()).thenReturn("secret");

        final Token accessToken = mock(Token.class);
        when(accessToken.getToken()).thenReturn("accessToken");
        when(accessToken.getSecret()).thenReturn("accessSecret");
        when(oAuthService.getAccessToken(eq(requestToken), any(Verifier.class))).thenReturn(accessToken);
        when(oAuthService.getRequestToken()).thenReturn(requestToken);
        when(oAuthService.getAuthorizationUrl(eq(requestToken))).thenReturn(SAMPLE_AUTH_URL);

        addBitbucketOrganization = new AddBitbucketOrganization(ap, eventPublisher, oAuthStore, organizationService, httpClientProvider)
        {
            @Override
            OAuthService createOAuthScribeService() {
                return oAuthService;
            }
        };
        addBitbucketOrganization.setUrl("http://url.com");
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
        addBitbucketOrganization.setSource(SAMPLE_SOURCE);
        String ret = addBitbucketOrganization.doExecute();
        assertThat(ret, equalTo(Action.NONE));
        verify(eventPublisher).publish(new DvcsConfigAddStartedAnalyticsEvent(SAMPLE_SOURCE, EVENT_TYPE_BITBUCKET));
        verifyNoMoreInteractions(eventPublisher);
        verify(response).sendRedirect(eq(SAMPLE_AUTH_URL));
        verifyNoMoreInteractions(response);
    }
    @Test
    public void testDoExecuteAnalyticsDefaultSource() throws Exception
    {
        addBitbucketOrganization.setSource(null);
        String ret = addBitbucketOrganization.doExecute();
        assertThat(ret, equalTo(Action.NONE));
        verify(eventPublisher).publish(new DvcsConfigAddStartedAnalyticsEvent(DEFAULT_SOURCE, EVENT_TYPE_BITBUCKET));
        verifyNoMoreInteractions(eventPublisher);
        verify(response).sendRedirect(eq(SAMPLE_AUTH_URL));
        verifyNoMoreInteractions(response);
    }

    @Test
    public void testDoExecuteAnalyticsError() throws Exception
    {
        addBitbucketOrganization.setSource(SAMPLE_SOURCE);
        reset(oAuthService);
        when(oAuthService.getRequestToken()).thenThrow(OAuthException.class);
        String ret = addBitbucketOrganization.doExecute();
        assertThat(ret, equalTo(Action.INPUT));
        verify(eventPublisher).publish(new DvcsConfigAddStartedAnalyticsEvent(SAMPLE_SOURCE, EVENT_TYPE_BITBUCKET));
        verify(eventPublisher).publish(new DvcsConfigAddEndedAnalyticsEvent(SAMPLE_SOURCE, EVENT_TYPE_BITBUCKET, OUTCOME_FAILED, FAILED_REASON_OAUTH_TOKEN));
        verifyNoMoreInteractions(eventPublisher);
        verifyNoMoreInteractions(response);
    }

    @Test
    public void testDoFinishAnalytics() throws Exception
    {
        addBitbucketOrganization.setSource(SAMPLE_SOURCE);
        String ret = addBitbucketOrganization.doFinish();
        assertThat(ret, equalTo(Action.NONE));
        verify(eventPublisher).publish(new DvcsConfigAddEndedAnalyticsEvent(SAMPLE_SOURCE, EVENT_TYPE_BITBUCKET, OUTCOME_SUCCEEDED, null));
        verifyNoMoreInteractions(eventPublisher);
        verify(response).sendRedirect(eq("ConfigureDvcsOrganizations.jspa?atl_token=" + SAMPLE_XSRF_TOKEN + "&source=" + SAMPLE_SOURCE));
        verifyNoMoreInteractions(response);
    }

    @Test
    public void testDoFinishAnalyticsDefaultSource() throws Exception
    {
        addBitbucketOrganization.setSource(null);
        String ret = addBitbucketOrganization.doFinish();
        assertThat(ret, equalTo(Action.NONE));
        verify(eventPublisher).publish(new DvcsConfigAddEndedAnalyticsEvent(DEFAULT_SOURCE, EVENT_TYPE_BITBUCKET, OUTCOME_SUCCEEDED, null));
        verifyNoMoreInteractions(eventPublisher);
        verify(response).sendRedirect(eq("ConfigureDvcsOrganizations.jspa?atl_token=" + SAMPLE_XSRF_TOKEN)); // source parameter skipped
        verifyNoMoreInteractions(response);
    }

    @Test
    public void testDoFinishAnalyticsErrorUnauth() throws Exception
    {
        addBitbucketOrganization.setSource(SAMPLE_SOURCE);
        when(organizationService.save(any(Organization.class))).thenThrow(SourceControlException.UnauthorisedException.class);
        String ret = addBitbucketOrganization.doFinish();
        assertThat(ret, equalTo(Action.INPUT));
        verify(eventPublisher).publish(new DvcsConfigAddEndedAnalyticsEvent(SAMPLE_SOURCE, EVENT_TYPE_BITBUCKET, OUTCOME_FAILED, FAILED_REASON_OAUTH_UNAUTH));
        verifyNoMoreInteractions(eventPublisher);
        verifyNoMoreInteractions(response);
    }

    @Test
    public void testDoFinishAnalyticsErrorSourceControl() throws Exception
    {
        addBitbucketOrganization.setSource(SAMPLE_SOURCE);
        reset(organizationService);
        when(organizationService.save(any(Organization.class))).thenThrow(SourceControlException.class);
        String ret = addBitbucketOrganization.doFinish();
        assertThat(ret, equalTo(Action.INPUT));
        verify(eventPublisher).publish(new DvcsConfigAddEndedAnalyticsEvent(SAMPLE_SOURCE, EVENT_TYPE_BITBUCKET, OUTCOME_FAILED, FAILED_REASON_OAUTH_SOURCECONTROL));
        verifyNoMoreInteractions(eventPublisher);
        verifyNoMoreInteractions(response);
    }

    @Test
    public void testDoValidationAnalyticsError() throws Exception
    {
        addBitbucketOrganization.setSource(SAMPLE_SOURCE);
        addBitbucketOrganization.setOrganization(null); // cause validation error
        addBitbucketOrganization.doValidation();
        verify(eventPublisher).publish(new DvcsConfigAddEndedAnalyticsEvent(SAMPLE_SOURCE, EVENT_TYPE_BITBUCKET, OUTCOME_FAILED, FAILED_REASON_VALIDATION));
        verifyNoMoreInteractions(eventPublisher);
    }

    @Test
    public void testDoValidationAnalyticsNoError() throws Exception
    {
        addBitbucketOrganization.setSource(SAMPLE_SOURCE);
        addBitbucketOrganization.setOrganization("org");
        final AccountInfo accountInfo = mock(AccountInfo.class);
        when(organizationService.getAccountInfo(anyString(), anyString(), Mockito.eq(BitbucketCommunicator.BITBUCKET))).thenReturn(accountInfo);
        addBitbucketOrganization.doValidation();
//        verify(eventPublisher).publish(new DvcsConfigAddEndedAnalyticsEvent(SAMPLE_SOURCE, EVENT_TYPE_BITBUCKET, OUTCOME_FAILED, FAILED_REASON_VALIDATION));
        verifyNoMoreInteractions(eventPublisher);
    }
}
