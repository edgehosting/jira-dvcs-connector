package com.atlassian.jira.plugins.dvcs.rest.common;

import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Status for http error responses
 */
@XmlRootElement
public class Status
{
    /**
     * The HTTP reponse code
     */
    @XmlElement(name = "status-code")
    private final Integer code;

    /**
     * Message for the given status.
     */
    @XmlElement
    private final String message;

    public Status()
    {
        this(Response.Status.OK, null);
    }

    public Status(Response.Status status, String message)
    {
        this.code = status.getStatusCode();
        this.message = message;
    }

    public int getCode()
    {
        return code;
    }

    public String getMessage()
    {
        return message;
    }
}
