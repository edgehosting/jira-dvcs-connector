package com.atlassian.jira.plugins.dvcs.dao;

import java.util.List;

import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.GlobalFilter;

public interface ChangesetDao
{
    void removeAllInRepository(int repositoryId);

    Changeset save(Changeset changeset);

    Changeset getByNode(int repositoryId, String changesetNode);

    List<Changeset> getByIssueKey(String issueKey);

    List<Changeset> getLatestChangesets(int maxResults, GlobalFilter gf);
    
    List<Changeset> getLatestChangesetsAvailableForSmartcommits();
    
    void forEachLatestChangesetsAvailableForSmartcommitDo(ForEachChangesetClosure closure);
    
    void markSmartcommitAvailability(int id, boolean available);
    
    public interface ForEachChangesetClosure {
    	void execute(Changeset changeset);
    }
}
