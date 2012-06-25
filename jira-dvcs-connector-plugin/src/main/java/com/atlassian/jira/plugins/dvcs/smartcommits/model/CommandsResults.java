package com.atlassian.jira.plugins.dvcs.smartcommits.model;

import java.util.HashMap;
import java.util.Map;

import com.atlassian.jira.plugins.dvcs.smartcommits.model.CommitCommands.CommitCommand;

public class CommandsResults
{

	Map<CommitCommand, CommandResult> results = new HashMap<CommitCommands.CommitCommand, CommandsResults.CommandResult>();
	
	public CommandsResults()
	{
		super();
	}
	
	public Map<CommitCommand, CommandResult> getResults()
	{
		return results;
	}

	public void addResult() {
		
	}

	public void setResults(Map<CommitCommand, CommandResult> results)
	{
		this.results = results;
	}



	public static class CommandResult {
		
		CommitCommand command;
		
		public CommandResult()
		{
			super();
		}
		
	}
	
}

