package com.atlassian.jira.plugins.dvcs.listener;

import com.atlassian.jira.plugins.dvcs.model.Group;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The Class UserAddedExternallyEventProcessor.
 * <p/>
 * {@link Runnable} processor that handles logic beside invitations for user added to JIRA i.e. via crowd so not via
 * user interface.
 * <p/>
 * <br /> <br /> Created on 21.6.2012, 15:22:43 <br /> <br />
 *
 * @author jhocman@atlassian.com
 */
public class UserAddedExternallyEventProcessor extends UserInviteCommonEventProcessor implements Runnable
{

    private static final Logger log = LoggerFactory.getLogger(UserAddedExternallyEventProcessor.class);

    private final OrganizationService organizationService;

    /**
     * The communicator provider.
     */
    private final DvcsCommunicatorProvider communicatorProvider;

    private final String username;

    public UserAddedExternallyEventProcessor(String username, OrganizationService organizationService,
            DvcsCommunicatorProvider communicatorProvider, UserManager userManager, GroupManager groupManager)
    {
        super(userManager, groupManager);

        this.username = username;
        this.organizationService = organizationService;
        this.communicatorProvider = communicatorProvider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run()
    {

        log.debug("Running UserAddedExternallyEventProcessor ...");

        ApplicationUser user = userManager.getUserByName(username);

        List<Organization> defaultOrganizations = organizationService.getAll(false);

        // continue ? ------------------------------------------
        if (CollectionUtils.isEmpty(defaultOrganizations))
        {
            return;
        }
        // ------------------------------------------------------

        for (Organization organization : defaultOrganizations)
        {
            Set<Group> groupSlugs = organization.getDefaultGroups();
            Set<String> slugsStrings = extractSlugs(groupSlugs);

            // log
            logInvite(user, slugsStrings);

            if (CollectionUtils.isNotEmpty(slugsStrings))
            {
                DvcsCommunicator communicator = communicatorProvider.getCommunicator(organization.getDvcsType());
                communicator.inviteUser(organization, slugsStrings, user.getEmailAddress());
            }
        }

    }

    /**
     * Extract slugs.
     *
     * @param groupSlugs the group slugs
     * @return the collection< string>
     */
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
