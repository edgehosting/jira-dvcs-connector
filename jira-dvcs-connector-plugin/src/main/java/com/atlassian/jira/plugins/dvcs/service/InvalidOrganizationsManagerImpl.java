package com.atlassian.jira.plugins.dvcs.service;

import java.util.List;

import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

public class InvalidOrganizationsManagerImpl implements InvalidOrganizationManager {
    
    private static final String SETTINGS_KEY = "dvcsInvalidOrganizations";
    
    private final PluginSettingsFactory pluginSettingsFactory;
    
    public InvalidOrganizationsManagerImpl(PluginSettingsFactory pluginSettingsFactory)
    {
        this.pluginSettingsFactory = pluginSettingsFactory;
    }
    
    /* (non-Javadoc)
     * @see com.atlassian.jira.plugins.dvcs.service.InvalidOrganizationManager#validateOrganization(int)
     */
    @Override
    public void validateOrganization(int organizationId)
    {
        List<String> invalidOrganizations = loadInvalidOrganizations();
        String organizationIdString =  Integer.toString(organizationId);
        if  (invalidOrganizations != null)
        {
            invalidOrganizations.remove(organizationIdString);
            pluginSettingsFactory.createGlobalSettings().put("dvcsInvalidOrganizations", invalidOrganizations);
        }
        
    }
    
    /* (non-Javadoc)
     * @see com.atlassian.jira.plugins.dvcs.service.InvalidOrganizationManager#invalidateOrganization(int)
     */
    @Override
    public void invalidateOrganization(int organizationId)
    {
        List<String> invalidOrganizations = loadInvalidOrganizations();
        String organizationIdString =  Integer.toString(organizationId);
        if (!invalidOrganizations.contains(organizationIdString) )
        {
            invalidOrganizations.add(organizationIdString);
            saveInvalidOrganizations(invalidOrganizations);
        }
    }
    
    /* (non-Javadoc)
     * @see com.atlassian.jira.plugins.dvcs.service.InvalidOrganizationManager#isInvalidOrganization(int)
     */
    @Override
    public boolean isInvalidOrganization(int organizationId)
    {
        List<String> invalidOrganizations = loadInvalidOrganizations();
        if (invalidOrganizations == null)
        {
            return false;
        }
        
        return invalidOrganizations.contains(Integer.toString(organizationId));
    }
    
    @SuppressWarnings("unchecked")
    private List<String> loadInvalidOrganizations()
    {
        return (List<String>) pluginSettingsFactory.createGlobalSettings().get(SETTINGS_KEY);
    }
    
    private void saveInvalidOrganizations(List<String> invalidOrganizations)
    {
        pluginSettingsFactory.createGlobalSettings().put(SETTINGS_KEY, invalidOrganizations);
    }
}
