package com.atlassian.jira.plugins.dvcs.spi.github.webwork;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubOAuth;
import com.atlassian.jira.plugins.dvcs.util.CustomStringUtils;
import com.atlassian.sal.api.ApplicationProperties;

public class GithubOAuthUtils {

	private final Logger log = LoggerFactory.getLogger(GithubOAuthUtils.class);

	private final GithubOAuth githubOAuth;
	private final ApplicationProperties applicationProperties;

    public GithubOAuthUtils(GithubOAuth githubOAuth, ApplicationProperties applicationProperties)
    {
		this.githubOAuth = githubOAuth;
		this.applicationProperties = applicationProperties;
	}

    public String createGithubRedirectUrl(String nextAction, String url, String xsrfToken,
            String organization, String autoLinking)
    {
		String encodedRepositoryUrl = encode(url);

		// Redirect back URL
		String redirectBackUrl = applicationProperties.getBaseUrl()
				+ "/secure/admin/" + nextAction + "!finish.jspa?url="
				+ encodedRepositoryUrl + "&atl_token=" + xsrfToken
				+ "&organization=" + organization + "&autoLinking="
				+ autoLinking;
		String encodedRedirectBackUrl = encode(redirectBackUrl);
		//
		// build URL to github
		//
		String githubAuthorizeUrl = "https://github.com/login/oauth/authorize?scope=repo&client_id="
				+ githubOAuth.getClientId()
				+ "&redirect_uri="
				+ encodedRedirectBackUrl;

		return githubAuthorizeUrl;
	}

    public String requestAccessToken(String code)
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
            String requestUrl = "https://github.com/login/oauth/access_token?&client_id="
                    + githubOAuth.getClientId() + "&client_secret=" + githubOAuth.getClientSecret()
                    + "&code=" + code;

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

    public static String encode(String url)
    {
        return CustomStringUtils.encode(url);
    }
}
