package com.atlassian.jira.plugins.bitbucket.spi.github.webwork;

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
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class AddGithubRepository extends JiraWebActionSupport
{
    private final Logger log = LoggerFactory.getLogger(AddGithubRepository.class);

    private String repositoryUrl;
    private String projectKey;
    private String isPrivate;
    private String adminUsername = "";
    private String adminPassword = "";
    private String bbUsername = "";
    private String bbPassword = "";
    private String code;

    private final RepositoryManager globalRepositoryManager;
    private final Synchronizer synchronizer;
    private final ApplicationProperties ap;
    private final GithubOAuth githubOAuth;
    

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
        return doExecute();
    }
    
    @Override
    @RequiresXsrfCheck
    protected String doExecute() throws Exception
    {
        try
        {
            if (isPrivate())
            {
                return redirectUserToGithub();
            } else
            {
                SourceControlRepository repository = globalRepositoryManager.addRepository(GithubRepositoryManager.GITHUB, projectKey, repositoryUrl, bbUsername, bbPassword,
                    adminUsername, adminPassword, "");
                synchronizer.synchronize(repository);
                // BBC-28 - Install postcommit service on github 
                // globalRepositoryManager.setupPostcommitHook(repository);
                return getRedirect("ConfigureBitbucketRepositories.jspa?atl_token=" + getXsrfToken());
            }
        } catch (SourceControlException e)
        {
            log.debug(e.getMessage(),e);
            addErrorMessage(e.getMessage());
            return INPUT;
        }
    }

    private String redirectUserToGithub()
    {
        if (StringUtils.isBlank(githubOAuth.getClientId()))
        {
            String oAuthSetupUrl = ap.getBaseUrl() + "/secure/admin/ConfigureGithubOAuth!default.jspa";
            addErrorMessage("OAuth needs to be <a href='"+oAuthSetupUrl+"' target='_blank'>configured</> before adding private github repository.");
            return INPUT;
        }
        String encodedRepositoryUrl = CustomStringUtils.encode(repositoryUrl);
        String encodedRedirectUrl = CustomStringUtils.encode(ap.getBaseUrl() + "/secure/admin/AddGithubRepository!finish.jspa?repositoryUrl="+encodedRepositoryUrl+"&projectKey="+projectKey+"&atl_token=" + getXsrfToken()); 
        String githubAuthorizeUrl = "https://github.com/login/oauth/authorize?scope=repo&client_id=" + githubOAuth.getClientId() + "&redirect_uri="+encodedRedirectUrl;
        return getRedirect(githubAuthorizeUrl);
    }

    
    public String doFinish()
    {
        try
        {
            String accessToken = requestAccessToken();
            SourceControlRepository repository = globalRepositoryManager.addRepository(GithubRepositoryManager.GITHUB, projectKey,
                repositoryUrl, "", "", "", "", accessToken);
            synchronizer.synchronize(repository);
            // BBC-28 - Install postcommit service on github 
            // globalRepositoryManager.setupPostcommitHook(repository);
            return getRedirect("ConfigureBitbucketRepositories.jspa?atl_token=" + getXsrfToken());
        } catch (SourceControlException e)
        {
            log.debug(e.getMessage(),e);
            addErrorMessage(e.getMessage());
            return INPUT;
        }
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
            e.printStackTrace();
        } catch (Exception e)
        {
            e.printStackTrace();
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

    public String getAdminUsername()
    {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername)
    {
        this.adminUsername = adminUsername;
    }

    public String getAdminPassword()
    {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword)
    {
        this.adminPassword = adminPassword;
    }

    public String getBbUsername()
    {
        return bbUsername;
    }

    public void setBbUsername(String bbUsername)
    {
        this.bbUsername = bbUsername;
    }

    public String getBbPassword()
    {
        return bbPassword;
    }

    public void setBbPassword(String bbPassword)
    {
        this.bbPassword = bbPassword;
    }

    public String getCode()
    {
        return code;
    }

    public void setCode(String code)
    {
        this.code = code;
    }
}
