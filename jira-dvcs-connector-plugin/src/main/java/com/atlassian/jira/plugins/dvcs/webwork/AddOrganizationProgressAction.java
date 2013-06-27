package com.atlassian.jira.plugins.dvcs.webwork;

import java.util.HashMap;
import java.util.Map;

import com.atlassian.jira.plugins.dvcs.util.SystemUtils;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.ApplicationProperties;

public class AddOrganizationProgressAction extends JiraWebActionSupport
{
	private static final long serialVersionUID = -8035393686536929940L;
	private final ApplicationProperties ap;
	private static Map<String, String> typeToRedirectUrls = new HashMap<String, String>();
	static
	{
		typeToRedirectUrls.put("1", "AddBitbucketOrganization");
		typeToRedirectUrls.put("2", "AddGithubOrganization");
		typeToRedirectUrls.put("3", "AddGithubEnterpriseOrganization");
	}

	public AddOrganizationProgressAction(ApplicationProperties ap)
	{
		super();
		this.ap = ap;
	}

	@Override
	public String doDefault() throws Exception
	{
		return super.doDefault();
	}

	public String doFinish() throws Exception
	{
		return doDefault();
	}

	public String getCurrentUrl()
	{
	    // using request directly because of compatibility with Jira 5.2
		String redirectEndpoint = typeToRedirectUrls.get(this.request.getParameter("t"));
		if (redirectEndpoint != null)
		{
			return ap.getBaseUrl()
			        + "/secure/admin/" + redirectEndpoint + "!finish.jspa?"
			        + this.request.getQueryString();
		} else
		{
			return SystemUtils.getRedirect(this, "ConfigureDvcsOrganizations.jspa?atl_token=" + getXsrfToken(), false);
		}
	}

}
