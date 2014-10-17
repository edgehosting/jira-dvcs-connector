package com.atlassian.jira.plugins.dvcs.webwork;

import com.atlassian.jira.plugins.dvcs.util.SystemUtils;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class AddOrganizationProgressAction extends JiraWebActionSupport
{
    private static final long serialVersionUID = -8035393686536929940L;
    private String t;
    private String redirectEndpoint;

    private static Map<String, String> typeToRedirectUrls = new HashMap<String, String>();

    static
    {
        typeToRedirectUrls.put("1", "AddBitbucketOrganization");
        typeToRedirectUrls.put("2", "AddGithubOrganization");
        typeToRedirectUrls.put("3", "AddGithubEnterpriseOrganization");
    }

    @Override
    public String doDefault() throws Exception
    {
        redirectEndpoint = typeToRedirectUrls.get(t);
        if (redirectEndpoint != null)
        {
            return super.doDefault();
        }
        else
        {
            return SystemUtils.getRedirect(this, "ConfigureDvcsOrganizations.jspa?atl_token=" + getXsrfToken(), false);
        }
    }

    public void setT(String t)
    {
        this.t = t;
    }

    public String getRedirectEndpoint()
    {
        return redirectEndpoint;
    }
}
