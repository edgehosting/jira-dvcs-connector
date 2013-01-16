package com.atlassian.jira.plugins.dvcs.webfragments;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.model.Group;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.opensymphony.util.TextUtils;

/**
 * The Class DefaultWebfragmentRenderer.
 */
public class DefaultWebfragmentRenderer implements WebfragmentRenderer
{
	private static final Logger log = LoggerFactory.getLogger(DefaultWebfragmentRenderer.class);

	/** The template renderer. */
	private final TemplateRenderer templateRenderer;

	/** The dvcs communicator provider. */
	private final DvcsCommunicatorProvider dvcsCommunicatorProvider;
	
	/** The organization service. */
	private final OrganizationService organizationService;

	/** The text utils. */
	private static TextUtils textUtils;
	
	private final ApplicationProperties appProperties;
	
	/**
	 * The Constructor.
	 *
	 * @param templateRenderer the template renderer
	 * @param dvcsCommunicatorProvider the dvcs communicator provider
	 * @param organizationService the organization service
	 */
	public DefaultWebfragmentRenderer(TemplateRenderer templateRenderer, DvcsCommunicatorProvider dvcsCommunicatorProvider,
			OrganizationService organizationService,
			ApplicationProperties appProperties)
	{
		super();
		this.templateRenderer = templateRenderer;
		this.dvcsCommunicatorProvider = dvcsCommunicatorProvider;
		this.organizationService = organizationService;
		this.appProperties = appProperties;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public String renderDefaultGroupsFragment (int orgId) throws IOException
	{
	
		StringWriter writer = new StringWriter();
		
		Map<String, Object> model = createDefaultGroupsModel(orgId);
		commonVariables(model);
		templateRenderer.render("templates/fragments/default-groups-fragment.vm", model, writer);
		
		return writer.toString();
		
	}

	@Override
	public String renderGroupsFragment() throws IOException
	{
		Map<String,Object> model = new HashMap<String, Object>();
        StringWriter stringWriter = new StringWriter();

        try
        {
            addBitbucketOrganizations(model);

            model.put("textutils", new TextUtils());
            model.put("baseurl", appProperties.getBaseUrl());

            templateRenderer.render("/templates/fragments/groups-fragment.vm", model, stringWriter);

        } catch (Exception e)
        {
            log.warn("Error while rendering DVCS extension fragment for add user form.", e);
            stringWriter = new StringWriter(); // reset writer so no broken
                                               // output goes out TODO should we print the error message to the writer?
        }

        return stringWriter.toString();
    }

    /**
     * Appends the bitbucket organizations to model.
     * 
     * @param model
     *            the model
     * @return the list
     */
    private List<Organization> addBitbucketOrganizations(Map<String, Object> model)
    {
        String dvcsType = "bitbucket";

        List<Organization> all = organizationService.getAll(false, dvcsType);
        Map<Integer, Set<String>> defaultSlugs = new HashMap<Integer, Set<String>>();

        DvcsCommunicator communicator = dvcsCommunicatorProvider.getCommunicator(dvcsType);

        boolean groupFound = false;

        List<Organization> listOfErrors = new ArrayList<Organization>();
        
        for (Organization organization : all)
        {
            try
            {
                Set<Group> groups = communicator.getGroupsForOrganization(organization);
                organization.setGroups(groups);

                groupFound |= CollectionUtils.isNotEmpty(groups);
                
                defaultSlugs.put(organization.getId(), extractSlugs(organization.getDefaultGroups()));

            } catch (Exception e)
            {
                log.warn("Failed to get groups for organization {}. Cause message is {}", organization.getName(),
                        e.getMessage());
                listOfErrors.add(organization);
            }
        }

        model.put("bbOrgaizations", all);
        model.put("defaultSlugs", defaultSlugs);

        if (!listOfErrors.isEmpty())
        {
        	 model.put("errors", listOfErrors);
        } else
        {
            // quick helper var to find out if we have som data to show
            model.put("bbSupressRender", !groupFound);
        }
        
        return all;
    }

    private Set<String> extractSlugs(Set<Group> groupSlugs)
    {
        Set<String> slugs = new HashSet<String>();

        if (groupSlugs == null)
        {
            return slugs;
        }

        for (Group group : groupSlugs)
        {
            slugs.add(group.getSlug());
        }
        return slugs;
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

