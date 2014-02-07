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
@XmlType(propOrder = { "id", "name", "events", "config" })
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubRepositoryHook
{

    /**
     * Name of "web" type hook.
     * 
     * @see #getName()
     */
    public static final String HOOK_NAME_WEB = "web";

    /**
     * Name of "push" event.
     * 
     * @see #getEvents()
     */
    public static final String EVENT_PUSH = "push";

    /**
     * @see #getId()
     */
    private Long id;

    /**
     * @see #getName()
     */
    private String name;

    /**
     * @see #getEvents()
     */
    private final List<String> events = new LinkedList<String>();

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
     * @return Config of hook.
     */
    public Map<String, String> getConfig()
    {
        return config;
    }

}
