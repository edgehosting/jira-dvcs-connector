package com.atlassian.jira.plugins.dvcs.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Used as arguments of rest methods instead of javax.ws.rs.FormParam
 * Handles i.e. checkbox checked value.
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class SentData
{
	
	private String payload;
	
	public SentData()
	{
		super();
	}

	public String getPayload()
	{
		return payload;
	}

	public void setPayload(String payload)
	{
		this.payload = payload;
	}

	
}
