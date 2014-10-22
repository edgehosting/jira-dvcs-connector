package com.atlassian.jira.plugins.dvcs.adduser;

import com.atlassian.jira.plugins.dvcs.listener.PluginFeatureDetector;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugin.web.model.AbstractWebPanel;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.templaterenderer.TemplateRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This panel extends the "add user" form in JIRA. It appends configured bitbucket
 * accounts with their groups so administrator can choose to which groups the
 * user being added should be invited.
 * 
 * An error rendering this panel will have no effect on the final "add user" form.
 * 
 * @author jhocman@atlassian.com
 */
@Scanned
public class AddUserDvcsExtensionWebPanel extends AbstractWebPanel
{
    private static final Logger log = LoggerFactory.getLogger(AddUserDvcsExtensionWebPanel.class);

    private final ApplicationProperties appProperties;
    private final PluginFeatureDetector featuresDetector;
    private final OrganizationService organizationService;
    private final TemplateRenderer templateRenderer;

    public AddUserDvcsExtensionWebPanel(@ComponentImport PluginAccessor pluginAccessor,
            @ComponentImport TemplateRenderer templateRenderer, @ComponentImport ApplicationProperties appProperties,
            PluginFeatureDetector featuresDetector, OrganizationService organizationService)
    {
        super(pluginAccessor);
        this.templateRenderer = checkNotNull(templateRenderer);
        this.appProperties = checkNotNull(appProperties);
        this.featuresDetector = checkNotNull(featuresDetector);
        this.organizationService = checkNotNull(organizationService);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHtml(Map<String, Object> model)
    {

        if (!featuresDetector.isUserInvitationsEnabled() || organizationService.getAllCount() == 0) 
        {
            return "";
        }

        StringWriter stringWriter = new StringWriter();
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
