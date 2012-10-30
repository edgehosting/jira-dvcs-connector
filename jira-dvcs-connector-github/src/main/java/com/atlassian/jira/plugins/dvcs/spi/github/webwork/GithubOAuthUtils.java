package com.atlassian.jira.plugins.dvcs.spi.github.webwork;

import static org.eclipse.egit.github.core.client.IGitHubConstants.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.lang.StringUtils;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubOAuth;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubOauthProvider;
import com.atlassian.jira.plugins.dvcs.util.CustomStringUtils;
import com.atlassian.sal.api.ApplicationProperties;

public class GithubOAuthUtils
{

    private final Logger log = LoggerFactory.getLogger(GithubOAuthUtils.class);

    private final GithubOAuth githubOAuth;
    private final ApplicationProperties applicationProperties;
    
    private final GithubOauthProvider oauthProvider;
    

    public GithubOAuthUtils(GithubOAuth githubOAuth, ApplicationProperties applicationProperties)
    {
        this(null, githubOAuth, applicationProperties);
    }

    public GithubOAuthUtils(GithubOauthProvider oauthProvider, GithubOAuth githubOAuth, ApplicationProperties applicationProperties)
    {
        this.oauthProvider = oauthProvider;
        this.githubOAuth = githubOAuth;
        this.applicationProperties = applicationProperties;
    }


    public String createGithubRedirectUrl(String nextAction, String url, String xsrfToken,
            String organization, String autoLinking, String autoSmartCommits)
    {
        String encodedRepositoryUrl = encode(url);

        // Redirect back URL
        String redirectBackUrl = applicationProperties.getBaseUrl() + "/secure/admin/" + nextAction
                + "!finish.jspa?url=" + encodedRepositoryUrl + "&atl_token=" + xsrfToken + "&organization="
                + organization + "&autoLinking=" + autoLinking + "&autoSmartCommits=" + autoSmartCommits;
        String encodedRedirectBackUrl = encode(redirectBackUrl);
        //
        // build URL to github
        //
        String githubAuthorizeUrl = url + "/login/oauth/authorize?scope=repo&client_id="
                + clentId() + "&redirect_uri=" + encodedRedirectBackUrl;

        return githubAuthorizeUrl;
    }

    public String requestAccessToken(String githubHostUrl, String code)
    {

        URL url;
        HttpURLConnection conn;

        BufferedReader rd;
        String line;
        String result = "";

        if (StringUtils.isEmpty(code))
        {
            throw new SourceControlException("Ops, no access code returned. Did you click Allow?");
        }

        try
        {
            String requestUrl = githubUrl(githubHostUrl) + "/login/oauth/access_token?&client_id="
                    + clentId() + "&client_secret=" + clientSecret() + "&code=" + code;

            log.debug("requestAccessToken() - " + requestUrl);

            url = new URL(requestUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(true);
            conn.setRequestMethod("POST");

            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while ((line = rd.readLine()) != null)
            {
                log.debug("RESPONSE: " + line);
                result += line;
            }
            rd.close();

        } catch (MalformedURLException e)
        {
            throw new SourceControlException("Error obtain access token.");

        } catch (Exception e)
        {
            throw new SourceControlException("Error obtain access token. Please check your credentials.");
        }

        if (result.startsWith("error="))
        {
            String errorCode = result.replaceAll("error=", "");
            String error = errorCode;
            if (errorCode.equals("incorrect_client_credentials"))
            {
                error = "Incorrect client credentials";
            } else if (errorCode.equals("bad_verification_code"))
            {
                error = "Bad verification code";
            }

            throw new SourceControlException("Error obtaining access token: " + error);
        }

        return result.replaceAll("access_token=(.*)&token_type.*", "$1");
    }


    private String githubUrl(String githubHostUrl)
    {
        return StringUtils.isNotBlank(githubHostUrl) ? githubHostUrl : "https://github.com";
    }

    public String requestAccessToken(String code)
    {

        return requestAccessToken(null, code);
    }

    public static String encode(String url)
    {
        return CustomStringUtils.encode(url);
    }
    
    private String clientSecret()
    {
        if (oauthProvider != null)
        {
            return oauthProvider.provideClientSecret();
        }
        return githubOAuth.getClientSecret();
    }
    
    private String clentId()
    {
        if (oauthProvider != null)
        {
            return oauthProvider.provideClientId();
        }
        return githubOAuth.getClientId();
    }
    
    /**
     * Create a GitHubClient to connect to the api.
     *
     * It uses the right host in case we're calling the github.com api.
     * It uses the right protocol in case we're calling the GitHub Enterprise api.
     *
     * @param url is the GitHub's oauth host.
     * @return a GitHubClient
     */
    public static GitHubClient createClient(String url) {
        try {
            URL urlObject = new URL(url);
            String host = urlObject.getHost();
            
            if (HOST_DEFAULT.equals(host) || HOST_GISTS.equals(host)) {
                host = HOST_API;
            }
            
            return new GitHubClient(host, -1, urlObject.getProtocol());
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
