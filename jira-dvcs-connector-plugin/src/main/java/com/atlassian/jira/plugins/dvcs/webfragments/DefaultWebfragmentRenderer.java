package com.atlassian.jira.plugins.dvcs.webfragments;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
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

    private final ApplicationProperties appProperties;

    /**
     * The Constructor.
     * 
     * @param templateRenderer
     *            the template renderer
     * @param dvcsCommunicatorProvider
     *            the dvcs communicator provider
     * @param organizationService
     *            the organization service
     */
    public DefaultWebfragmentRenderer(TemplateRenderer templateRenderer, DvcsCommunicatorProvider dvcsCommunicatorProvider,
            OrganizationService organizationService, ApplicationProperties appProperties)
    {
        super();
        this.templateRenderer = templateRenderer;
        this.dvcsCommunicatorProvider = dvcsCommunicatorProvider;
        this.organizationService = organizationService;
        this.appProperties = appProperties;
    }

    @Override
    public String renderGroupsFragmentForAddUser() throws IOException
    {
        Map<String, Object> model = new HashMap<String, Object>();
        StringWriter stringWriter = new StringWriter();

        addBitbucketOrganizations(model);

        model.put("textutils", new TextUtils());
        model.put("baseurl", appProperties.getBaseUrl());

        templateRenderer.render("/templates/fragments/groups-fragment.vm", model, stringWriter);

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
                List<Group> groups = communicator.getGroupsForOrganization(organization);
                organization.setGroups(groups);

                groupFound |= CollectionUtils.isNotEmpty(groups);

                defaultSlugs.put(organization.getId(), extractSlugs(organization.getDefaultGroups()));

            } catch (Exception e)
            {
                log.warn("Failed to get groups for organization {}. Cause message is {}", organization.getName(), e.getMessage());
                listOfErrors.add(organization);
            }
        }

        model.put("bbOrganizations", all);
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

}
