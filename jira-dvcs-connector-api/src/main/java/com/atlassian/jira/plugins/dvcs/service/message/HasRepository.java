package com.atlassian.jira.plugins.dvcs.service.message;

import com.atlassian.jira.plugins.dvcs.model.Repository;

public interface HasRepository
{
    Repository getRepository();
}
