package com.atlassian.jira.plugins.dvcs.spi.bitbucket.webwork;

import org.apache.commons.lang.StringUtils;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.SignatureType;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.auth.OAuthStore;
import com.atlassian.jira.plugins.dvcs.auth.OAuthStore.Host;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketOAuthAuthentication;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.util.DebugOutputStream;
import com.atlassian.jira.plugins.dvcs.util.SystemUtils;
import com.atlassian.jira.plugins.dvcs.webwork.RegenerateOauthTokenAction;
import com.atlassian.sal.api.ApplicationProperties;

public class RegenerateBitbucketOauthToken extends RegenerateOauthTokenAction
{
    private static final long serialVersionUID = -2316358416248237835L;

    private final Logger log = LoggerFactory.getLogger(RegenerateBitbucketOauthToken.class);
    
    private final ApplicationProperties ap;

    public RegenerateBitbucketOauthToken(OrganizationService organizationService, RepositoryService repositoryService, ApplicationProperties ap,
            OAuthStore oAuthStore)
    {
        super(organizationService, repositoryService, oAuthStore);
        this.ap = ap;
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
            addErrorMessage("Cannot proceed authentication, check your OAuth credentials!");
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

        ServiceBuilder sb = new ServiceBuilder().apiKey(oAuthStore.getClientId(Host.BITBUCKET.id))
                .signatureType(SignatureType.Header).apiSecret(oAuthStore.getSecret(Host.BITBUCKET.id))
                .provider(new Bitbucket10aScribeApi(organizationInstance.getHostUrl()))
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
        Token accessTokenObj = service.getAccessToken(requestToken, verifier);
        return BitbucketOAuthAuthentication.generateAccessTokenString(accessTokenObj);
    }
}
