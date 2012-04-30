package com.atlassian.jira.plugins.dvcs.webwork;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import com.atlassian.crowd.model.event.UserEvent;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.event.web.action.admin.UserAddedEvent;

public class DvcsAddUserListener implements InitializingBean {
	
    private static final Logger log = LoggerFactory.getLogger(DvcsAddUserListener.class);

    public static String ORGANIZATION_SELECTOR_REQUEST_PARAM = "dvcs_org_selector";
    
    private final EventPublisher eventPublisher;

    public DvcsAddUserListener(EventPublisher eventPublisher) {
    	super();
		this.eventPublisher = eventPublisher;
    }
    
    @EventListener
    public void onUserAddViaInterface(UserAddedEvent event) {
    	Map<String, String[]> parameters = event.getRequestParameters();
    	// TODO LOG what we've got 
    }
    
    @EventListener
    public void onUserAddViaCrowd(UserEvent event) {
    	
    }

	@Override
	public void afterPropertiesSet() throws Exception {
		eventPublisher.register(this);
	}
    
    /*
    public IssueCreatedResolvedListener(EventPublisher eventPublisher) {
        eventPublisher.register(this);    // Demonstration only -- don't do this in real code!
    }
 
    @EventListener
    public void onIssueEvent(IssueEvent issueEvent) {
        Long eventTypeId = issueEvent.getEventTypeId();
        Issue issue = issueEvent.getIssue();
 
        // if it's an event we're interested in, log it
        if (eventTypeId.equals(EventType.ISSUE_CREATED_ID)) {
            log.info("Issue {} has been created at {}.", issue.getKey(), issue.getCreated());
        } else if (eventTypeId.equals(EventType.ISSUE_RESOLVED_ID)) {
            log.info("Issue {} has been resolved at {}.", issue.getKey(), issue.getResolutionDate());
        }
    }*/
}