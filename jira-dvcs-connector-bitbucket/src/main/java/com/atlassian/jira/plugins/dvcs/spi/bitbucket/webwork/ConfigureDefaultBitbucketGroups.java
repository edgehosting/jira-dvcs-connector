package com.atlassian.jira.plugins.dvcs.spi.bitbucket.webwork;

import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.util.CustomStringUtils;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class ConfigureDefaultBitbucketGroups extends JiraWebActionSupport
{
	private static final long serialVersionUID = 6246027331604675862L;

	final Logger logger = LoggerFactory.getLogger(ConfigureDefaultBitbucketGroups.class);
	
	private String organizationIdDefaultGroups;
	
	private String [] organizationDefaultGroups;

	private final OrganizationService organizationService;

    @Autowired
    public ConfigureDefaultBitbucketGroups(OrganizationService organizationService)
    {
        this.organizationService = organizationService;
    }

    @Override
    protected void doValidation()
    {
    	
    }

    @Override
    @RequiresXsrfCheck
    protected String doExecute() throws Exception
    {
    	
    	List<String> slugs = new ArrayList<String>();

    	if (organizationDefaultGroups != null && organizationDefaultGroups.length > 0) {
    		slugs.addAll(Arrays.asList(organizationDefaultGroups));
    	}
    	
    	organizationService.setDefaultGroupsSlugs(Integer.parseInt(organizationIdDefaultGroups), slugs);

        return getRedirect("ConfigureDvcsOrganizations.jspa?atl_token=" + CustomStringUtils.encode(getXsrfToken()));
    }

	public String getOrganizationIdDefaultGroups()
	{
		return organizationIdDefaultGroups;
	}

	public void setOrganizationIdDefaultGroups(String organizationIdDefaultGroups)
	{
		this.organizationIdDefaultGroups = organizationIdDefaultGroups;
	}

	public String[] getOrganizationDefaultGroups()
	{
		return organizationDefaultGroups;
	}

	public void setOrganizationDefaultGroups(String[] organizationDefaultGroups)
	{
		this.organizationDefaultGroups = organizationDefaultGroups;
	}



}
