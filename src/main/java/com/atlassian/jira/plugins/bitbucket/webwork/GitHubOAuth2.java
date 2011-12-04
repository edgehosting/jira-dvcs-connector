package com.atlassian.jira.plugins.bitbucket.webwork;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

/**
 * Webwork action to handle OAuth callback URL from github configured from instructions in jira-github-connector
 * Remove in the future versions of the plugin. 
 */
@Deprecated 
public class GitHubOAuth2 extends JiraWebActionSupport
{
    private final Logger log = LoggerFactory.getLogger(GitHubOAuth2.class);

    private final PluginSettingsFactory pluginSettingsFactory;

    private String code;
    private String error;
    private String redirectUrl;

    private final ApplicationProperties applicationProperties;

    public GitHubOAuth2(PluginSettingsFactory pluginSettingsFactory, ApplicationProperties applicationProperties)
    {
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.applicationProperties = applicationProperties;
    }

    @Override
    protected String doExecute() throws Exception
    {
       log.debug("Handling old (from jira-github-connector) callback Url. Arguments are error: ["+error+"], code=["+code+"]");
       
       if ("redirect_uri_mismatch".equals(error))
       {
           // github didn't like our redirect url because the currently registed application is github has points elsewhere
           redirectUrl = (String) pluginSettingsFactory.createGlobalSettings().get("OAuthRedirectUrl");
           return getRedirect(redirectUrl);
       }
       
       String parameters = (String) pluginSettingsFactory.createGlobalSettings().get("OAuthRedirectUrlParameters");
       redirectUrl = "/secure/admin/AddGithubRepository!finish.jspa?"+parameters+"&code="+code;

       // clean up
       pluginSettingsFactory.createGlobalSettings().remove("OAuthRedirectUrl");
       pluginSettingsFactory.createGlobalSettings().remove("OAuthRedirectUrlParameters");
       return SUCCESS;
    }

    public String getRedirectUrl()
    {
        return redirectUrl;
    }

    public String getError()
    {
        return error;
    }

    public void setError(String error)
    {
        this.error = error;
    }

    public String getCode()
    {
        return code;
    }

    public void setCode(String code)
    {
        this.code = code;
    }
    
    public String getBaseUrl()
    {
        return applicationProperties.getBaseUrl();
    }
}
