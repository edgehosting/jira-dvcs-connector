package com.atlassian.jira.plugins.bitbucket.spi.github.webwork;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import com.atlassian.jira.plugins.bitbucket.Synchronizer;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlException;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.spi.CustomStringUtils;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryManager;
import com.atlassian.jira.plugins.bitbucket.spi.github.GithubOAuth;
import com.atlassian.jira.plugins.bitbucket.spi.github.impl.GithubRepositoryManager;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

public class AddGithubRepository extends JiraWebActionSupport
{
    private final Logger log = LoggerFactory.getLogger(AddGithubRepository.class);

    private String repositoryUrl;
    private String projectKey;
    private String isPrivate;

    private String addPostCommitService ;
    private String code;

    private final RepositoryManager globalRepositoryManager;
    private final Synchronizer synchronizer;
    private final ApplicationProperties ap;
    private final GithubOAuth githubOAuth;
    private String postCommitUrl;
    private String accessToken = "";

    private final PluginSettingsFactory pluginSettingsFactory;


    public AddGithubRepository(@Qualifier("globalRepositoryManager") RepositoryManager globalRepositoryManager, Synchronizer synchronizer, 
        ApplicationProperties applicationProperties, GithubOAuth githubOAuth, PluginSettingsFactory pluginSettingsFactory)
    {
        this.globalRepositoryManager = globalRepositoryManager;
        this.synchronizer = synchronizer;
        this.ap = applicationProperties;
        this.githubOAuth = githubOAuth;
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

    @Override
    protected void doValidation()
    {
    }

    @Override
    public String doDefault() throws Exception
    {
        if (oAuthIsNeeded()) {
            checkOAuthConfig();
        }
        return INPUT;
    }

    @Override
    @RequiresXsrfCheck
    protected String doExecute() throws Exception
    {
        if (oAuthIsNeeded()) {
            checkOAuthConfig();
            if (hasAnyErrors()) {
                return INPUT;
            }
            return redirectUserToGithub();
        }

        return doAddRepository();

    }

    private void checkOAuthConfig() {
        if (StringUtils.isBlank(githubOAuth.getClientId()))
        {
            String oAuthSetupUrl = ap.getBaseUrl() + "/secure/admin/ConfigureGithubOAuth!default.jspa";
            if (isPrivate())
            {
                addErrorMessage("OAuth needs to be <a href='"+oAuthSetupUrl+"' target='_blank'>configured</a> before adding private github repository.");
            } else
            {
                addErrorMessage("OAuth needs to be <a href='"+oAuthSetupUrl+"' target='_blank'>configured</a> before install postcommit service.");
            }
        }
    }

    private boolean oAuthIsNeeded()
    {
        return isPrivate() || addPostCommitService();
    }


    private String redirectUserToGithub()
    {
        String encodedRepositoryUrl = CustomStringUtils.encode(repositoryUrl);
        String redirectBackUrl = ap.getBaseUrl() + "/secure/admin/AddGithubRepository!finish.jspa?repositoryUrl=" + encodedRepositoryUrl
            + "&projectKey=" + projectKey + "&addPostCommitService=" + addPostCommitService() + "&atl_token=" + getXsrfToken();
        String encodedRedirectBackUrl = CustomStringUtils.encode(redirectBackUrl);
        String githubAuthorizeUrl = "https://github.com/login/oauth/authorize?scope=repo&client_id=" + githubOAuth.getClientId()
            + "&redirect_uri=" + encodedRedirectBackUrl;
        
        fixBackwardCompatibility();

        return getRedirect(githubAuthorizeUrl);
    }

    /**
     * TODO add detailed comment what is this for.
     * @param redirectBackUrl
     */
    private void fixBackwardCompatibility()
    {
        String encodedRepositoryUrl = CustomStringUtils.encode(repositoryUrl);
        String parameters = "repositoryUrl=" + encodedRepositoryUrl + "&projectKey=" + projectKey + "&addPostCommitService="
            + addPostCommitService() + "&atl_token=" + getXsrfToken();
        String redirectBackUrl = ap.getBaseUrl() + "/secure/admin/GitHubOAuth2.jspa?" + parameters;
        String encodedRedirectBackUrl = CustomStringUtils.encode(redirectBackUrl);
        String githubAuthorizeUrl = "https://github.com/login/oauth/authorize?scope=repo&client_id=" + githubOAuth.getClientId()
            + "&redirect_uri=" + encodedRedirectBackUrl;        
        
        pluginSettingsFactory.createGlobalSettings().put("OAuthRedirectUrl", githubAuthorizeUrl); 
        pluginSettingsFactory.createGlobalSettings().put("OAuthRedirectUrlParameters", parameters); 
    }

    
    public String doFinish()
    {
        try
        {
            accessToken = requestAccessToken();
        } catch (SourceControlException sce)
        {
            addErrorMessage(sce.getMessage());
            return INPUT;
        }

        return doAddRepository();
    }

    private String doAddRepository()
    {
        SourceControlRepository repository;
        try
        {
            repository = globalRepositoryManager.addRepository(GithubRepositoryManager.GITHUB, projectKey, repositoryUrl, "", "",
                "", "", accessToken);
            synchronizer.synchronize(repository);

        } catch (SourceControlException e)
        {
            addErrorMessage("Failed adding the repository: ["+e.getMessage()+"]");
            log.debug("Failed adding the repository: ["+e.getMessage()+"]");
            return INPUT;
        }

        try
        {
            if (addPostCommitService())
                globalRepositoryManager.setupPostcommitHook(repository);
        } catch (SourceControlException e)
        {
            log.debug("Failed adding postcommit hook: ["+e.getMessage()+"]");
            postCommitUrl = ap.getBaseUrl() + "/rest/bitbucket/1.0/repository/" + repository.getId() + "/sync";
            return ERROR;
        }

        return getRedirect("ConfigureBitbucketRepositories.jspa?addedRepositoryId="+repository.getId()+"&atl_token=" + getXsrfToken());
    }

    // TODO rewrite this nicely (using RequestFactory)
    private String requestAccessToken()
    {
        URL url;
        HttpURLConnection conn;

        BufferedReader rd;
        String line;
        String result = "";

        if (StringUtils.isEmpty(code)) {
            throw new SourceControlException("Ops, no access code returned. Did you click Allow?");
        }

        try
        {
            log.debug("requestAccessToken() - " + "https://github.com/login/oauth/access_token?&client_id=" + githubOAuth.getClientId()
                + "&client_secret=" + githubOAuth.getClientSecret() + "&code=" + code);

            url = new URL("https://github.com/login/oauth/access_token?&client_id=" + githubOAuth.getClientId() + "&client_secret="
                + githubOAuth.getClientSecret() + "&code=" + code);
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
            log.error("Error obtain access token", e);
        } catch (Exception e)
        {
            log.error("Error obtain access token", e);
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

        result = result.replaceAll("access_token=(.*)&token_type.*", "$1");

        return result;
    }

    public String getRepositoryUrl()
    {
        return repositoryUrl;
    }

    public void setRepositoryUrl(String repositoryUrl)
    {
        this.repositoryUrl = repositoryUrl;
    }

    public String getProjectKey()
    {
        return projectKey;
    }

    public void setProjectKey(String projectKey)
    {
        this.projectKey = projectKey;
    }

    public boolean isPrivate()
    {
        return Boolean.parseBoolean(isPrivate);
    }

    public void setIsPrivate(String isPrivate)
    {
        this.isPrivate = isPrivate;
    }

    public String getCode()
    {
        return code;
    }

    public void setCode(String code)
    {
        this.code = code;
    }

    public boolean addPostCommitService()
    {
        return addPostCommitService != null && (addPostCommitService.toLowerCase().equals("on") || addPostCommitService.toLowerCase().equals("true"));
    }

    public void setAddPostCommitService(String addPostCommitService)
    {
        this.addPostCommitService = addPostCommitService;
    }

    public String getPostCommitUrl()
    {
        return postCommitUrl;
    }

    public void setPostCommitUrl(String postCommitUrl)
    {
        this.postCommitUrl = postCommitUrl;
    }
}
