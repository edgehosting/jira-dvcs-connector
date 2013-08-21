package com.atlassian.jira.plugins.dvcs.webwork;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugin.issuetabpanel.IssueAction;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.ContextProvider;

public class DvcsTabPanelContextProvider implements ContextProvider {

    private final Logger logger = LoggerFactory.getLogger(DvcsTabPanelContextProvider.class);

    private final ChangesetRenderer changesetRenderer;

    public DvcsTabPanelContextProvider(ChangesetRenderer changesetRenderer) {
        this.changesetRenderer = changesetRenderer;
    }

    @Override
    public void init(Map<String, String> stringStringMap) throws PluginParseException {
    }

    @Override
    public Map<String, Object> getContextMap(Map<String, Object> context) {
        Issue issue = (Issue) context.get("issue");

        StringBuffer sb = new StringBuffer();

        Long items = 0L;

        List<IssueAction> actions = Collections.EMPTY_LIST;
        try {
            actions = changesetRenderer.getAsActions(issue);
            boolean isNoMessages = isNoMessages(actions);
            if (isNoMessages)
            {
                sb.append("<div class=\"ghx-container\"><p class=\"ghx-fa\">" + ChangesetRenderer.DEFAULT_MESSAGE_GH_TXT + "</p></div>");
            } else
            {
                for (IssueAction issueAction : actions) {
                    sb.append(issueAction.getHtml());
                    items += 1;
                }
            }
        } catch (SourceControlException e) {
            logger.debug("Could not retrieve changeset for [ " + issue.getKey() + " ]: " + e, e);
        }

        HashMap<String, Object> params = new HashMap<String, Object>();

        params.put("renderedChangesetsWithHtml", sb.toString());
        params.put("atl.gh.issue.details.tab.count", items);

        return params;
    }

    private boolean isNoMessages(List<IssueAction> actions) {
        return actions.isEmpty();
    }
}
