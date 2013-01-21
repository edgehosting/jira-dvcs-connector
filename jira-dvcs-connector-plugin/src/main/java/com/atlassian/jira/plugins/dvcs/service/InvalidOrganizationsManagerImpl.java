package com.atlassian.jira.plugins.dvcs.service;

import java.util.List;

import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

public class InvalidOrganizationsManagerImpl implements InvalidOrganizationManager {
    
    private static final String SETTINGS_KEY = "dvcsInvalidOrganizations";
    
    private final PluginSettingsFactory pluginSettingsFactory;
    
    public InvalidOrganizationsManagerImpl(PluginSettingsFactory pluginSettingsFactory)
    {
        this.pluginSettingsFactory = pluginSettingsFactory;
    }
    
    @Override
    public void setOrganizationValid(int organizationId, boolean valid) {
    	if ( valid )
    	{
    		validateOrganization(organizationId);
    	} else
    	{
    		invalidateOrganization(organizationId);
    	}
    	
    }

    private void validateOrganization(int organizationId)
    {
        List<String> invalidOrganizations = loadInvalidOrganizations();
        String organizationIdString =  Integer.toString(organizationId);
        if  (invalidOrganizations != null)
        {
            invalidOrganizations.remove(organizationIdString);
            pluginSettingsFactory.createGlobalSettings().put("dvcsInvalidOrganizations", invalidOrganizations);
        }
        
    }
    
    private void invalidateOrganization(int organizationId)
    {
        List<String> invalidOrganizations = loadInvalidOrganizations();
        String organizationIdString =  Integer.toString(organizationId);
        if (!invalidOrganizations.contains(organizationIdString) )
        {
            invalidOrganizations.add(organizationIdString);
            saveInvalidOrganizations(invalidOrganizations);
        }
    }
    
    @Override
    public boolean isOrganizationValid(int organizationId)
    {
        List<String> invalidOrganizations = loadInvalidOrganizations();
        if (invalidOrganizations == null)
        {
            return true;
        }
        
        return !invalidOrganizations.contains(Integer.toString(organizationId));
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
