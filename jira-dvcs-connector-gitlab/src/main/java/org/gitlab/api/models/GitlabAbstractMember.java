package org.gitlab.api.models;

import org.codehaus.jackson.annotate.JsonProperty;

public abstract class GitlabAbstractMember extends GitlabUser {

    public static final String URL = "/members";

    @JsonProperty("access_level")
    private int accessLevel;

    public Integer getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(Integer accessLevel) {
        this.accessLevel = accessLevel;
    }

}
