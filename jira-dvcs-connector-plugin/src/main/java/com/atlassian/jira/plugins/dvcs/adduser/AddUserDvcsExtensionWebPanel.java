package com.atlassian.jira.plugins.dvcs.adduser;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.model.Group;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.atlassian.plugin.web.model.WebPanel;
import com.atlassian.templaterenderer.TemplateRenderer;

/**
 * This panel extends user-add form in JIRA. It appends
 * configured bitbucket accounts with theirs groups so administrator
 * can choose to which goups should be currently added user invited.
 *
 * Error during rendering this panel has no effect to final add-user form.
 * 
 * <br /><br />
 * Created on 21.6.2012, 15:40:43
 * <br /><br />
 * @author jhocman@atlassian.com
 *
 */
public class AddUserDvcsExtensionWebPanel implements WebPanel
{

	/** The Constant log. */
	private static final Logger log = LoggerFactory.getLogger(AddUserDvcsExtensionWebPanel.class);

	/** The template renderer. */
	private final TemplateRenderer templateRenderer;

	/** The organization service. */
	private final OrganizationService organizationService;

	/** The communicator provider. */
	private final DvcsCommunicatorProvider communicatorProvider;

	/**
	 * Instantiates a new adds the user dvcs extension web panel.
	 *
	 * @param templateRenderer the template renderer
	 * @param organizationService the organization service
	 * @param communicatorProvider the communicator provider
	 */
	public AddUserDvcsExtensionWebPanel(TemplateRenderer templateRenderer, OrganizationService organizationService,
			DvcsCommunicatorProvider communicatorProvider)
	{
		this.templateRenderer = templateRenderer;
		this.organizationService = organizationService;
		this.communicatorProvider = communicatorProvider;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getHtml(Map<String, Object> model)
	{

		StringWriter stringWriter = new StringWriter();

		try
		{

			addBitbucketOrganizations(model);
			templateRenderer.render("/templates/dvcs/add-user-dvcs-extension.vm", model, stringWriter);

		} catch (Exception e)
		{
			log.warn("Error while rendering DVCS extension fragment for add user form.", e);
			stringWriter = new StringWriter(); // reset writer so no broken
												// output goes out
		}

		return stringWriter.toString();
	}

	/**
	 * Appends the bitbucket organizations to model.
	 *
	 * @param model the model
	 * @return the list
	 */
	private List<Organization> addBitbucketOrganizations(Map<String, Object> model)
	{
		String dvcsType = "bitbucket";

		List<Organization> all = organizationService.getAll(false, dvcsType);
		DvcsCommunicator communicator = communicatorProvider.getCommunicator(dvcsType);

		for (Organization organization : all)
		{
			List<Group> groups = communicator.getGroupsForOrganization(organization);
			organization.setGroups(groups);
		}

		model.put("bbOrgaizations", all);
		
		// quick helper var to find out if we have som data to show
		model.put("bbSupressRender", all.isEmpty());

		return all;

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void writeHtml(Writer writer, Map<String, Object> model) throws IOException
	{

	}

}
