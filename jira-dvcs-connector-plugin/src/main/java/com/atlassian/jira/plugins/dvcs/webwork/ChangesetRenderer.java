package com.atlassian.jira.plugins.dvcs.webwork;

import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.Repository;

import java.util.Map;

public interface ChangesetRenderer {

    public String getHtmlForChangeset(Changeset changeset);

    public Map<String, Object> getVelocityContextForChangeset(Changeset changeset, Repository repository);
}
