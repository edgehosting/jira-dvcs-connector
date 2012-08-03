package com.atlassian.jira.plugins.dvcs.webfragments;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import com.atlassian.jira.plugins.dvcs.model.Group;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.opensymphony.util.TextUtils;

/**
 * The Class DefaultWebfragmentRenderer.
 */
public class DefaultWebfragmentRenderer implements WebfragmentRenderer
{

	/** The template renderer. */
	private final TemplateRenderer templateRenderer;

	/** The dvcs communicator provider. */
	private final DvcsCommunicatorProvider dvcsCommunicatorProvider;
	
	/** The organization service. */
	private final OrganizationService organizationService;

	/** The text utils. */
	private static TextUtils textUtils;
	
	/**
	 * The Constructor.
	 *
	 * @param templateRenderer the template renderer
	 * @param dvcsCommunicatorProvider the dvcs communicator provider
	 * @param organizationService the organization service
	 */
	public DefaultWebfragmentRenderer(TemplateRenderer templateRenderer, DvcsCommunicatorProvider dvcsCommunicatorProvider,
			OrganizationService organizationService)
	{
		super();
		this.templateRenderer = templateRenderer;
		this.dvcsCommunicatorProvider = dvcsCommunicatorProvider;
		this.organizationService = organizationService;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public String renderDefaultGroupsFragment (int orgId) throws IOException {
	
		StringWriter writer = new StringWriter();
		
		Map<String, Object> model = createDefaultGroupsModel(orgId);
		commonVariables(model);
		templateRenderer.render("templates/fragments/default-groups-fragment.vm", model, writer);
		
		return writer.toString();
		
	}


	/**
	 * Creates the default groups model.
	 *
	 * @param orgId the org id
	 * @return the map< string, object>
	 */
	private Map<String, Object> createDefaultGroupsModel(int orgId)
	{
		Organization organization = organizationService.get(orgId, false);
		DvcsCommunicator communicator = dvcsCommunicatorProvider.getCommunicator(organization.getDvcsType());
		Set<Group> groups = communicator.getGroupsForOrganization(organization);
		
		//
		HashMap<String, Object> model = new HashMap<String, Object>();
		model.put("organization", organization);
		model.put("groups", groups);
		model.put("configuredSlugs", createExistingSlugsSet(organization));
		
		return model;
	}
	
    @SuppressWarnings("unchecked")
	private Set<String> createExistingSlugsSet(Organization organization)
	{
        Collection<String> existingSlugs = CollectionUtils.collect(organization.getDefaultGroups(), new Transformer() {

            @Override
            public Object transform(Object input)
            {
                Group group = (Group) input;
                
                return group.getSlug();
            }
        });
        
        return new HashSet<String>(existingSlugs);
	}

	/**
	 * Common variables.
	 *
	 * @param model the model
	 */
	private void commonVariables(Map<String, Object> model)
	{
		textUtils = new TextUtils();
		model.put("textutils", textUtils);
	}
}

