package com.atlassian.jira.plugins.dvcs.spi.github.webwork;

import static org.eclipse.egit.github.core.client.IGitHubConstants.*;

import java.io.BufferedReader;
import java.io.DataOutputStream;
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
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException.InvalidResponseException;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubOAuth;
import com.atlassian.jira.plugins.dvcs.util.CustomStringUtils;
import com.atlassian.sal.api.ApplicationProperties;

public class GithubOAuthUtils
{

    private final Logger log = LoggerFactory.getLogger(GithubOAuthUtils.class);

    private final GithubOAuth githubOAuth;
    private final ApplicationProperties applicationProperties;

    public GithubOAuthUtils(GithubOAuth githubOAuth, ApplicationProperties applicationProperties)
    {
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
                + githubOAuth.getClientId() + "&redirect_uri=" + encodedRedirectBackUrl;

        return githubAuthorizeUrl;
    }

    public String requestAccessToken(String githubHostUrl, String code)
    {
        log.debug("Requesting access token at " + githubHostUrl + " with code " + code);
        
        URL url;
        HttpURLConnection conn;

        BufferedReader rd;
        String line;
        String result = "";

        if (StringUtils.isEmpty(code))
        {
            throw new SourceControlException("Ops, no access code returned. Did you click Allow?");
        }

        String githubUrl = githubUrl(githubHostUrl);
        try
        {
            String requestUrl = githubUrl + "/login/oauth/access_token";

            String urlParameters = "client_id=" + githubOAuth.getClientId() + "&client_secret=" + githubOAuth.getClientSecret() + "&code=" + code;
            
            log.debug("requestAccessToken() - " + requestUrl + " with parameters " + urlParameters);

            url = new URL(requestUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); 
            conn.setRequestProperty("charset", "utf-8");
            conn.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
            conn.setUseCaches (false);

            DataOutputStream wr = new DataOutputStream(conn.getOutputStream ());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();
            
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while ((line = rd.readLine()) != null)
            {
                log.debug("RESPONSE: " + line);
                result += line;
            }
            rd.close();

        } catch (MalformedURLException e)
        {
            throw new SourceControlException("Error obtaining access token.",e);

        } catch (IOException ioe)
        {
            throw new SourceControlException("Error obtaining access token. Cannot access " + githubUrl + " from Jira.",ioe);
        }
        catch (Exception e)
        {
            throw new SourceControlException("Error obtaining access token. Please check your credentials.",e);
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
        } else if (!result.startsWith("access_token"))
        {
            log.error("Requested access token response is invalid");
            throw new InvalidResponseException("Error obtaining access token. Response is invalid.");
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
            
            GitHubClient result = new GitHubClient(host, -1, urlObject.getProtocol());
            // FIXME: should be fixed properly - via plugin name and their version
            // this one is just hot fix
            result.setUserAgent("JIRA DVCS Connector 1.4.x");
            return result;
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
