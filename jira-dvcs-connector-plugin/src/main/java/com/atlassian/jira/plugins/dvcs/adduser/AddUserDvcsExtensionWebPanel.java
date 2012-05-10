package com.atlassian.jira.plugins.dvcs.adduser;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.plugin.web.model.WebPanel;
import com.atlassian.templaterenderer.TemplateRenderer;

public class AddUserDvcsExtensionWebPanel implements WebPanel {

	private static final Logger log = LoggerFactory.getLogger(AddUserDvcsExtensionWebPanel.class);

	private final TemplateRenderer templateRenderer;

	private final OrganizationService organizationService;

	public AddUserDvcsExtensionWebPanel(TemplateRenderer templateRenderer, OrganizationService organizationService) {
		this.templateRenderer = templateRenderer;
		this.organizationService = organizationService;
	}

	@Override
	public String getHtml(Map<String, Object> model) {
		
		StringWriter stringWriter = new StringWriter();
	
		try {
			
			addBitbucketOrganizations(model);
			templateRenderer.render("/templates/dvcs/add-user-dvcs-extension.vm", model, stringWriter);
			
		} catch (Exception e) {
			log.warn("Error while rendering DVCS extension fragment for add user form.", e);
			stringWriter = new StringWriter(); // reset writer so no broken output goes out
		} 
		
		return stringWriter.toString();
	}

	private List<Organization> addBitbucketOrganizations(Map<String, Object> model)
	{
		
		List<Organization> all = organizationService.getAll(false);
		List<Organization> bitbucketOrganizations = new ArrayList<Organization>();
		for (Organization organization : all)
		{	
			if (organization.getDvcsType().equals("bitbucket")) {
				bitbucketOrganizations.add(organization);
			}
		}
		
		model.put("bbOrgaizations", bitbucketOrganizations);
		return bitbucketOrganizations;
		
	}

	@Override
	public void writeHtml(Writer writer, Map<String, Object> model)
			throws IOException {

	}

}
