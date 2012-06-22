package com.atlassian.jira.plugins.dvcs.smartcommits;

import java.io.Serializable;

public class PayloadChangeset implements Serializable
{

	private static final long serialVersionUID = 3102461512644192998L;

	private String commitMessage;
	
	private String author;
	
	public PayloadChangeset()
	{
		super();
	}

	public String getCommitMessage()
	{
		return commitMessage;
	}

	public void setCommitMessage(String commitMessage)
	{
		this.commitMessage = commitMessage;
	}

	public String getAuthor()
	{
		return author;
	}

	public void setAuthor(String author)
	{
		this.author = author;
	}
	
	
}

