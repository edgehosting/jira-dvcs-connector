package com.atlassian.jira.plugins.bitbucket.api.model.v3;

import net.java.ao.Entity;

public interface Organization extends Entity {
	
	String getUri();
    String getUsername();
    String getPassword();

    void setUri(String owner);
    void setUsername(String username);
    void setPassword(String password);

}
