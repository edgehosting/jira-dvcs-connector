package com.atlassian.jira.plugins.dvcs.webwork;

import com.atlassian.jira.config.FeatureManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.PluginInformation;
import org.junit.Assert;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * Basic set of tests for PanelVisibilityManager
 *
 * @since v6.0
 */
public class PanelVisibilityManagerTest
{

    private static final String DEVSUMMARY_PLUGIN_ID = "com.atlassian.jira.plugins.jira-development-integration-plugin";
    private static final String LABS_OPT_IN = "jira.plugin.devstatus.phasetwo";

    @Mock
    PermissionManager permissionManager;

    @Mock
    PluginAccessor pluginAccessor;

    @Mock
    FeatureManager featureManager;

    @Mock
    Plugin fusionPlugin;

    @Mock
    PluginInformation fusionPluginInfo;

    @Mock
    private Issue issue;

    @Mock
    private ApplicationUser user;

    @BeforeClass
    public void initMocks()
    {
        MockitoAnnotations.initMocks(this);
    }


    @Test
    public void visibleWhenFusionPluginDisabled() throws Exception
    {
        PanelVisibilityManager panelVisibilityManager = new PanelVisibilityManager(permissionManager, pluginAccessor, featureManager);

        //given
        // the fusion plugin is disabled
        when(pluginAccessor.isPluginEnabled(eq(DEVSUMMARY_PLUGIN_ID))).thenReturn(false);

        // but all other settings tell the tab to hide
        when(featureManager.isEnabled(LABS_OPT_IN)).thenReturn(true);
        when(pluginAccessor.getPlugin(DEVSUMMARY_PLUGIN_ID)).thenReturn(fusionPlugin);
        when(fusionPlugin.getPluginInformation()).thenReturn(fusionPluginInfo);
        when(fusionPluginInfo.getVersion()).thenReturn("1.0.0");

        // except the permission manager which is ANDed.
        when(permissionManager.hasPermission(ProjectPermissions.VIEW_DEV_TOOLS, issue, user)).thenReturn(true);

        //when
        Assert.assertThat(panelVisibilityManager.showPanel(issue, user), is(equalTo(true)));
    }

    @Test
    public void visibleWhenNoLabsFlag() throws Exception
    {
        PanelVisibilityManager panelVisibilityManager = new PanelVisibilityManager(permissionManager, pluginAccessor, featureManager);

        //given
        // the no labs flags disabled
        when(featureManager.isEnabled(LABS_OPT_IN)).thenReturn(false);

        // but all other settings tell the tab to hide
        when(pluginAccessor.isPluginEnabled(eq(DEVSUMMARY_PLUGIN_ID))).thenReturn(true);
        when(pluginAccessor.getPlugin(DEVSUMMARY_PLUGIN_ID)).thenReturn(fusionPlugin);
        when(fusionPlugin.getPluginInformation()).thenReturn(fusionPluginInfo);
        when(fusionPluginInfo.getVersion()).thenReturn("1.0.0");

        // except the permission manager which is ANDed.
        when(permissionManager.hasPermission(Permissions.VIEW_VERSION_CONTROL, issue, user)).thenReturn(true);

        //when
        Assert.assertThat(panelVisibilityManager.showPanel(issue, user), is(equalTo(true)));
    }

    @Test
    public void visibleWhenPluginTooOld() throws Exception
    {
        PanelVisibilityManager panelVisibilityManager = new PanelVisibilityManager(permissionManager, pluginAccessor, featureManager);

        //given
        // the version number is too old
        when(fusionPluginInfo.getVersion()).thenReturn("0.0.1");

        // but all other settings tell the tab to hide
        when(featureManager.isEnabled(LABS_OPT_IN)).thenReturn(true);
        when(pluginAccessor.isPluginEnabled(eq(DEVSUMMARY_PLUGIN_ID))).thenReturn(true);
        when(pluginAccessor.getPlugin(DEVSUMMARY_PLUGIN_ID)).thenReturn(fusionPlugin);
        when(fusionPlugin.getPluginInformation()).thenReturn(fusionPluginInfo);

        // except the permission manager which is ANDed.
        when(permissionManager.hasPermission(Permissions.VIEW_VERSION_CONTROL, issue, user)).thenReturn(true);

        //when
        Assert.assertThat(panelVisibilityManager.showPanel(issue, user), is(equalTo(true)));
    }

    @Test
    void hiddenWithPermissionButAllOthersSayHide()
    {
        PanelVisibilityManager panelVisibilityManager = new PanelVisibilityManager(permissionManager, pluginAccessor, featureManager);

        // All other settings tell the tab to hide
        when(featureManager.isEnabled(LABS_OPT_IN)).thenReturn(true);
        when(pluginAccessor.isPluginEnabled(eq(DEVSUMMARY_PLUGIN_ID))).thenReturn(true);
        when(pluginAccessor.getPlugin(DEVSUMMARY_PLUGIN_ID)).thenReturn(fusionPlugin);
        when(fusionPlugin.getPluginInformation()).thenReturn(fusionPluginInfo);
        when(fusionPluginInfo.getVersion()).thenReturn("1.0.1");

        // except the permission manager which is ANDed.
        when(permissionManager.hasPermission(Permissions.VIEW_VERSION_CONTROL, issue, user)).thenReturn(true);

        //when
        Assert.assertThat(panelVisibilityManager.showPanel(issue, user), is(equalTo(false)));

    }

    @Test
    public void hiddenWhenNoPermissionButAllOthersSayShow()
    {
        PanelVisibilityManager panelVisibilityManager = new PanelVisibilityManager(permissionManager, pluginAccessor, featureManager);

        //given
        // except the permission manager which is ANDed.
        when(permissionManager.hasPermission(Permissions.VIEW_VERSION_CONTROL, issue, user)).thenReturn(false);

        // but all other settings tell the tab to show
        when(featureManager.isEnabled(LABS_OPT_IN)).thenReturn(true);
        when(pluginAccessor.isPluginEnabled(eq(DEVSUMMARY_PLUGIN_ID))).thenReturn(true);
        when(pluginAccessor.getPlugin(DEVSUMMARY_PLUGIN_ID)).thenReturn(fusionPlugin);
        when(fusionPlugin.getPluginInformation()).thenReturn(fusionPluginInfo);
        when(fusionPluginInfo.getVersion()).thenReturn("1.0.1");


        //when
        Assert.assertThat(panelVisibilityManager.showPanel(issue, user), is(equalTo(false)));
    }

}
