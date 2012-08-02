package com.atlassian.jira.plugins.dvcs.adduser;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.listener.PluginFeatureDetector;
import com.atlassian.jira.plugins.dvcs.model.Group;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.web.model.AbstractWebPanel;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.opensymphony.util.TextUtils;

/**
 * This panel extends user-add form in JIRA. It appends configured bitbucket
 * accounts with theirs groups so administrator can choose to which goups should
 * be currently added user invited.
 * 
 * Error during rendering this panel has no effect to final add-user form.
 * 
 * <br />
 * <br />
 * Created on 21.6.2012, 15:40:43 <br />
 * <br />
 * 
 * @author jhocman@atlassian.com
 * 
 */
public class AddUserDvcsExtensionWebPanel extends AbstractWebPanel
{

    /** The Constant log. */
    private static final Logger log = LoggerFactory.getLogger(AddUserDvcsExtensionWebPanel.class);

    /** The template renderer. */
    private final TemplateRenderer templateRenderer;

    /** The organization service. */
    private final OrganizationService organizationService;

    /** The communicator provider. */
    private final DvcsCommunicatorProvider communicatorProvider;

    private final ApplicationProperties appProperties;

    private final PluginFeatureDetector featuresDetector;

    /**
     * Instantiates a new adds the user dvcs extension web panel.
     * 
     * @param templateRenderer
     *            the template renderer
     * @param organizationService
     *            the organization service
     * @param communicatorProvider
     *            the communicator provider
     */
    public AddUserDvcsExtensionWebPanel(PluginAccessor pluginAccessor, OrganizationService organizationService,
            DvcsCommunicatorProvider communicatorProvider, TemplateRenderer templateRenderer,
            ApplicationProperties appProperties, PluginFeatureDetector featuresDetector)
    {
        super(pluginAccessor);
        this.organizationService = organizationService;
        this.communicatorProvider = communicatorProvider;
        this.templateRenderer = templateRenderer;
        this.appProperties = appProperties;
        this.featuresDetector = featuresDetector;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHtml(Map<String, Object> model)
    {

        StringWriter stringWriter = new StringWriter();
        
        if (!featuresDetector.isUserInvitationsEnabled()) 
        {
            return stringWriter.toString();
        }

        try
        {

            addBitbucketOrganizations(model);

            model.put("textutils", new TextUtils());
            model.put("baseurl", appProperties.getBaseUrl());

            templateRenderer.render("/templates/dvcs/add-user-dvcs-extension.vm", model, stringWriter);

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

        DvcsCommunicator communicator = communicatorProvider.getCommunicator(dvcsType);

        boolean groupFound = false;

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
            }
        }

        model.put("bbOrgaizations", all);
        model.put("defaultSlugs", defaultSlugs);

        // quick helper var to find out if we have som data to show
        model.put("bbSupressRender", !groupFound);

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
