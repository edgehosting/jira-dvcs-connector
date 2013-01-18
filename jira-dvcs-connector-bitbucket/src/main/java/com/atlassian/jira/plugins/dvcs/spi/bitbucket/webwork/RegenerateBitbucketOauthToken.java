package com.atlassian.jira.plugins.dvcs.spi.bitbucket.webwork;

import org.apache.commons.lang.StringUtils;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.SignatureType;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketOAuth;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketOAuthAuthentication;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.util.DebugOutputStream;
import com.atlassian.jira.plugins.dvcs.util.CustomStringUtils;
import com.atlassian.jira.plugins.dvcs.util.SystemUtils;
import com.atlassian.jira.plugins.dvcs.webwork.CommonDvcsConfigurationAction;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.sal.api.ApplicationProperties;

public class RegenerateBitbucketOauthToken extends CommonDvcsConfigurationAction
{
    private static final long serialVersionUID = -2316358416248237835L;

    private final Logger log = LoggerFactory.getLogger(RegenerateBitbucketOauthToken.class);

    private String organization; // in the meaning of id

    private String accessToken = "";

    private final OrganizationService organizationService;

    private final ApplicationProperties ap;

    private final BitbucketOAuth bitbucketOauth;

    public RegenerateBitbucketOauthToken(OrganizationService organizationService, ApplicationProperties ap,
            BitbucketOAuth bitbucketOauth)
    {
        this.organizationService = organizationService;
        this.ap = ap;
        this.bitbucketOauth = bitbucketOauth;
    }

    @Override
    @RequiresXsrfCheck
    protected String doExecute() throws Exception
    {
        return redirectUserToBitbucket();
    }

    private String redirectUserToBitbucket()
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

        ServiceBuilder sb = new ServiceBuilder().apiKey(bitbucketOauth.getClientId())
                .signatureType(SignatureType.Header).apiSecret(bitbucketOauth.getClientSecret())
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
        if (StringUtils.isBlank(organization)) {
            
            addErrorMessage("No id has been provided, invalid request");
            
        } else {
            
            Organization integratedAccount = organizationService.findIntegratedAccount();
            if (    integratedAccount != null 
                &&  Integer.valueOf(organization).equals(integratedAccount.getId())) {
                
                addErrorMessage("Failed to regenerate token for an integrated account.");
                
            }

        }
    }

    public String doFinish()
    {

        Verifier verifier = new Verifier(request.getParameter("oauth_verifier"));
        Token requestToken = (Token) request.getSession().getAttribute("requestToken");
        request.getSession().removeAttribute("requestToken");

        OAuthService service = createOAuthScribeService();
        Token accessTokenObj = service.getAccessToken(requestToken, verifier);
        accessToken = BitbucketOAuthAuthentication.generateAccessTokenString(accessTokenObj);

        return doChangeAccessToken();
    }

    private String doChangeAccessToken()
    {
        try
        {
            organizationService.updateCredentialsAccessToken(Integer.parseInt(organization), accessToken);

        } catch (SourceControlException e)
        {
            addErrorMessage("Failed adding the account: [" + e.getMessage() + "]");
            log.debug("Failed adding the account: [" + e.getMessage() + "]");
            return INPUT;
        }

        return getRedirect("ConfigureDvcsOrganizations.jspa?atl_token=" + getXsrfToken());
    }

    public static String encode(String url)
    {
        return CustomStringUtils.encode(url);
    }

    public String getOrganization()
    {
        return organization;
    }

    public void setOrganization(String organization)
    {
        this.organization = organization;
    }

}
