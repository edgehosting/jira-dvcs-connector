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


    public AddGithubRepository(@Qualifier("globalRepositoryManager") RepositoryManager globalRepositoryManager, Synchronizer synchronizer, 
        ApplicationProperties applicationProperties, GithubOAuth githubOAuth)
    {
        this.globalRepositoryManager = globalRepositoryManager;
        this.synchronizer = synchronizer;
        this.ap = applicationProperties;
        this.githubOAuth = githubOAuth;
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
                addErrorMessage("OAuth needs to be <a href='"+oAuthSetupUrl+"' target='_blank'>configured</a> before install postcomit service.");
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
        String encodedRedirectBackUrl = CustomStringUtils.encode(ap.getBaseUrl()
            + "/secure/admin/AddGithubRepository!finish.jspa?repositoryUrl=" + encodedRepositoryUrl + "&projectKey=" + projectKey
            + "&addPostCommitService=" + addPostCommitService() + "&atl_token=" + getXsrfToken());
        String githubAuthorizeUrl = "https://github.com/login/oauth/authorize?scope=repo&client_id=" + githubOAuth.getClientId()
            + "&redirect_uri=" + encodedRedirectBackUrl;
        return getRedirect(githubAuthorizeUrl);
    }

    
    public String doFinish()
    {
        accessToken = requestAccessToken();
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
