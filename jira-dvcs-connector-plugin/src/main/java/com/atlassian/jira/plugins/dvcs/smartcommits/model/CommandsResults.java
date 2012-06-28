package com.atlassian.jira.plugins.dvcs.smartcommits.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.atlassian.jira.plugins.dvcs.smartcommits.model.CommitCommands.CommitCommand;

public class CommandsResults
{

	private List<String> globalErrors = new ArrayList<String>();
	
	private Map<CommitCommand, CommandResult> results = new HashMap<CommitCommands.CommitCommand, CommandsResults.CommandResult>();
	
	public CommandsResults()
	{
		super();
	}
	
	public Map<CommitCommand, CommandResult> getResults()
	{
		return results;
	}

	public void addResult(CommitCommand command, CommandResult result) {
		results.put(command, result);
	}

	public void setResults(Map<CommitCommand, CommandResult> results)
	{
		this.results = results;
	}
	
	public List<String> getGlobalErrors()
	{
		return globalErrors;
	}

	public void setGlobalErrors(List<String> globalErrors)
	{
		this.globalErrors = globalErrors;
	}
	
	public void addGlobalError(String message) {
		globalErrors.add(message);
	}
	
	public boolean hasErrors() {
		
		boolean hasErrors = !globalErrors.isEmpty();
		
		for (CommitCommand command : results.keySet())
		{	
			if (hasErrors) {
				break;
			}
			hasErrors |= !results.get(command).getErrors().isEmpty();
		}
		
		return hasErrors;
	}
	
	@Override
	public String toString()
	{
		return ReflectionToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
	}

	public static class CommandResult {
		
		private CommitCommand command;
		private List<String> errors = new ArrayList<String>();
		
		public CommandResult()
		{
			super();
		}

		public CommitCommand getCommand()
		{
			return command;
		}

		public void setCommand(CommitCommand command)
		{
			this.command = command;
		}

		public List<String> getErrors()
		{
			return errors;
		}

		public void setErrors(List<String> errors)
		{
			this.errors = errors;
		}
		
		public void addError(String message) {
			errors.add(message);
		}
		
	}

	
}

