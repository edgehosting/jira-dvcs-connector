package com.atlassian.jira.plugins.dvcs.webwork;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.web.action.JiraWebActionSupport;

public class CommonDvcsConfigurationAction extends JiraWebActionSupport {
	private static final long serialVersionUID = 8695500426304238626L;

	private final Logger logger = LoggerFactory
			.getLogger(CommonDvcsConfigurationAction.class);

	public CommonDvcsConfigurationAction() {
		super();
	}
	
}
