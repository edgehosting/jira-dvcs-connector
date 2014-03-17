package com.atlassian.jira.plugins.dvcs.github.api.model;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * GitHub repository.
 * 
 * @author Stanislav Dvorscak
 * 
 */
@XmlRootElement
@XmlType(propOrder = { "id", "name", "active", "events", "config" })
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubRepositoryHook
{
    
    /**
     * @see #getConfig()
     */
    public static final String CONFIG_URL = "url";
    
    /**
     * @see #getConfig()
     */
    public static final String CONFIG_CONTENT_TYPE = "content_type";

    /**
     * @see #getConfig()
     */
    public static final String CONFIG_CONTENT_TYPE_JSON = "json";
    
    /**
     * @see #getEvents()
     */
    public static final String EVENT_PUSH = "push";
    
    /**
     * @see #getEvents()
     */
    public static final String EVENT_PULL_REQUEST = "pull_request";

    /**
     * @see #getEvents()
     */
    public static final String EVENT_PULL_REQUEST_REVIEW_COMMENT = "pull_request_review_comment";
    /**
     * @see #getEvents()
     */
    public static final String EVENT_ISSUE_COMMENT = "issue_comment";

    /**
     * Name of "web" type hook.
     * 
     * @see #getName()
     */
    public static final String NAME_WEB = "web";

    /**
     * @see #getId()
     */
    private Long id;

    /**
     * @see #getName()
     */
    private String name;

    /**
     * 
     */
    private boolean active;

    /**
     * @see #getEvents()
     */
    private List<String> events = new LinkedList<String>();

    /**
     * @see #getConfig()
     */
    private Map<String, String> config = new HashMap<String, String>();

    /**
     * Constructor.
     */
    public GitHubRepositoryHook()
    {
    }

    /**
     * @return Identity of hook.
     */
    public Long getId()
    {
        return id;
    }

    /**
     * @param id
     *            {@link #getId()}
     */
    public void setId(Long id)
    {
        this.id = id;
    }

    /**
     * @return True if this hook is active (false if disabled/inactive).
     */
    public boolean isActive()
    {
        return active;
    }

    /**
     * @param active
     *            {@link #isActive()}
     */
    public void setActive(boolean active)
    {
        this.active = active;
    }

    /**
     * List o availables names: <a href="https://api.github.com/hooks">https://api.github.com/hooks</a>
     * 
     * @return Name of this hook
     */
    public String getName()
    {
        return name;
    }

    /**
     * @param name
     *            {@link #getName()}
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * List o availables events, which are supported by this hook of {@link #getName()} are : <a
     * href="https://api.github.com/hooks">https://api.github.com/hooks</a>
     * 
     * @return List of events, which are supported by this hook.
     */
    public List<String> getEvents()
    {
        return events;
    }

    /**
     * @param events
     *            {@link #getEvents()}
     */
    public void setEvents(List<String> events)
    {
        this.events = events;
    }

    /**
     * @return Config of hook.
     */
    public Map<String, String> getConfig()
    {
        return config;
    }

    /**
     * @param config
     *            {@link #getConfig()}
     */
    public void setConfig(Map<String, String> config)
    {
        this.config = config;
    }

}
