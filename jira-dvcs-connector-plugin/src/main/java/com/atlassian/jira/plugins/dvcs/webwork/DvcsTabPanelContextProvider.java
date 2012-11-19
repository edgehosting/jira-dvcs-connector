package com.atlassian.jira.plugins.dvcs.webwork;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.ContextProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DvcsTabPanelContextProvider implements ContextProvider {

    private final Logger logger = LoggerFactory.getLogger(DvcsTabPanelContextProvider.class);

    private final ChangesetService changesetService;
    private final ChangesetRenderer changesetRenderer;

    public DvcsTabPanelContextProvider(ChangesetService changesetService, ChangesetRenderer changesetRenderer) {
        this.changesetService = changesetService;
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

        try
        {
            for (Changeset changeset : changesetService.getByIssueKey(issue.getKey()))
            {
                logger.debug("found changeset [ {} ] on issue [ {} ]", changeset.getNode(), issue.getKey());

                String changesetHtml = changesetRenderer.getHtmlForChangeset(changeset);

                sb.append(changesetHtml);
                items += 1;
            }
        } catch (SourceControlException e)
        {
            logger.debug("Could not retrieve changeset for [ " + issue.getKey() + " ]: " + e, e);
        }

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("renderedChangesetsWithHtml", sb.toString());
        params.put("atl.gh.issue.details.tab.count", items);

        return params;
    }
}
