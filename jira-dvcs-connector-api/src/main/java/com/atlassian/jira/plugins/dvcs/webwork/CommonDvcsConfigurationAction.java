package com.atlassian.jira.plugins.dvcs.webwork;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.web.action.JiraWebActionSupport;

public class CommonDvcsConfigurationAction extends JiraWebActionSupport
{

	private String autoLinking = "";

	private static final long serialVersionUID = 8695500426304238626L;

	private final Logger logger = LoggerFactory.getLogger(CommonDvcsConfigurationAction.class);

	public CommonDvcsConfigurationAction()
	{
		super();
	}

	protected boolean hadAutolinkingChecked()
	{
		return StringUtils.isNotBlank(autoLinking);
	}

	public String getAutoLinking()
	{
		return autoLinking;
	}

	public void setAutoLinking(String autoLinking)
	{
		this.autoLinking = autoLinking;
	}

}
