package com.atlassian.jira.plugins.dvcs.adduser;

import java.io.StringWriter;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.listener.PluginFeatureDetector;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.web.model.AbstractWebPanel;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.templaterenderer.TemplateRenderer;

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
    public AddUserDvcsExtensionWebPanel(PluginAccessor pluginAccessor, TemplateRenderer templateRenderer,
            ApplicationProperties appProperties, PluginFeatureDetector featuresDetector)
    {
        super(pluginAccessor);
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
}
