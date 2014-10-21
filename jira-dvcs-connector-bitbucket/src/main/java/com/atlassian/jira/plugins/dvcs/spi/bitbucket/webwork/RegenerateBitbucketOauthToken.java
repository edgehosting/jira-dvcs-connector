package com.atlassian.jira.plugins.dvcs.spi.bitbucket.webwork;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketOAuthAuthentication;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.HttpClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.util.DebugOutputStream;
import com.atlassian.jira.plugins.dvcs.util.SystemUtils;
import com.atlassian.jira.plugins.dvcs.webwork.RegenerateOauthTokenAction;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.ApplicationProperties;
import org.apache.commons.lang.StringUtils;
import org.scribe.builder.ServiceBuilder;
import org.scribe.exceptions.OAuthConnectionException;
import org.scribe.model.SignatureType;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RegenerateBitbucketOauthToken extends RegenerateOauthTokenAction
{
    private final Logger log = LoggerFactory.getLogger(RegenerateBitbucketOauthToken.class);
    private final ApplicationProperties ap;
    private final HttpClientProvider httpClientProvider;

    @Autowired
    public RegenerateBitbucketOauthToken(@ComponentImport EventPublisher eventPublisher,
            OrganizationService organizationService, RepositoryService repositoryService,
            @ComponentImport ApplicationProperties ap, HttpClientProvider httpClientProvider)
    {
        super(eventPublisher, organizationService, repositoryService);
        this.ap = ap;
        this.httpClientProvider = httpClientProvider;
    }

    @Override
    protected String redirectUserToGrantAccess()
    {
        try
        {
            OAuthService service = createOAuthScribeService();
            Token requestToken = service.getRequestToken();
            String authUrl = service.getAuthorizationUrl(requestToken);

            request.getSession().setAttribute("requestToken", requestToken);

            return SystemUtils.getRedirect(this, authUrl, true);

        } catch (Exception e)
        {
            addErrorMessage("Cannot proceed authentication, check OAuth credentials for account " + getOrganizationName());
            return INPUT;
        }
    }

    private OAuthService createOAuthScribeService()
    {
        String redirectBackUrl = ap.getBaseUrl()
                + "/secure/admin/RegenerateBitbucketOauthToken!finish.jspa?organization=" + organization
                + "&atl_token=" + getXsrfToken();

        return createBitbucketOAuthScribeService(redirectBackUrl);
    }

    private OAuthService createBitbucketOAuthScribeService(String callbackUrl)
    {
        Organization organizationInstance = organizationService.get(Integer.parseInt(organization), false);

        ServiceBuilder sb = new ServiceBuilder().apiKey(organizationInstance.getCredential().getOauthKey())
                .signatureType(SignatureType.Header).apiSecret(organizationInstance.getCredential().getOauthSecret())
                .provider(new Bitbucket10aScribeApi(organizationInstance.getHostUrl(), httpClientProvider))
                .debugStream(new DebugOutputStream(log));

        if (!StringUtils.isBlank(callbackUrl))
        {
            sb.callback(callbackUrl);
        }

        return sb.build();
    }

    @Override
    protected void doValidation()
    {
        if (StringUtils.isBlank(organization))
        {
            addErrorMessage("No organization id has been provided, invalid request");
        } else
        {
            //TODO what if we have more integrated accounts?
            Organization integratedAccount = organizationService.findIntegratedAccount();
            if (    integratedAccount != null
                &&  Integer.valueOf(organization).equals(integratedAccount.getId()))
            {
                addErrorMessage("Failed to regenerate token for an integrated account.");
            }
        }
    }

    @Override
    protected String getAccessToken()
    {
        Verifier verifier = new Verifier(request.getParameter("oauth_verifier"));
        Token requestToken = (Token) request.getSession().getAttribute("requestToken");
        request.getSession().removeAttribute("requestToken");

        OAuthService service = createOAuthScribeService();
        Token accessTokenObj = null;
        try
        {
            accessTokenObj = service.getAccessToken(requestToken, verifier);
        } catch (OAuthConnectionException e)
        {
            Organization organizationInstance = organizationService.get(Integer.parseInt(organization), false);
            throw new SourceControlException("Error obtaining access token. Cannot access " + organizationInstance.getHostUrl() + " from Jira.", e);
        } finally
        {
            httpClientProvider.closeIdleConnections();
        }

        return BitbucketOAuthAuthentication.generateAccessTokenString(accessTokenObj);
    }
}
