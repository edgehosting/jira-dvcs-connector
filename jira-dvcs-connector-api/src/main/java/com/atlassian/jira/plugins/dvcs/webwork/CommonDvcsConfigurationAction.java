package com.atlassian.jira.plugins.dvcs.webwork;

import java.util.HashMap;

import org.apache.commons.lang.StringUtils;

import com.atlassian.jira.web.action.JiraWebActionSupport;

public class CommonDvcsConfigurationAction extends JiraWebActionSupport
{

	private String autoLinking = "";
	private String autoSmartCommits = "";

	private static final long serialVersionUID = 8695500426304238626L;

	protected static HashMap<String, String> dvcsTypeToUrlMap = new HashMap<String, String>();
	static {
		dvcsTypeToUrlMap.put("bitbucket", "https://bitbucket.org");
		dvcsTypeToUrlMap.put("github", "https://github.com");
	}

	public CommonDvcsConfigurationAction()
	{
		super();
	}

	protected boolean hadAutolinkingChecked()
	{
		return StringUtils.isNotBlank(autoLinking);
	}

	protected boolean hadAutoSmartCommitsChecked()
	{
	    return StringUtils.isNotBlank(autoSmartCommits);
	}

	public String getAutoLinking()
	{
		return autoLinking;
	}

	public void setAutoLinking(String autoLinking)
	{
		this.autoLinking = autoLinking;
	}

    public String getAutoSmartCommits()
    {
        return autoSmartCommits;
    }

    public void setAutoSmartCommits(String autoSmartCommits)
    {
        this.autoSmartCommits = autoSmartCommits;
    }
    

}
