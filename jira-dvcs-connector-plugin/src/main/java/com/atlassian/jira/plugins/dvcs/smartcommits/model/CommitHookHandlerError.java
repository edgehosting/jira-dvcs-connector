package com.atlassian.jira.plugins.dvcs.smartcommits.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.atlassian.jira.util.ErrorCollection;

public class CommitHookHandlerError {

	List<CommitHookError> errors;

    public CommitHookHandlerError() {
        errors = new ArrayList<CommitHookError>();
    }

    public static CommitHookHandlerError fromSingleError(String command, String issueKey, String message) {
        CommitHookHandlerError e = new CommitHookHandlerError();
        e.addError(command, issueKey, message);
        return e;
    }

    public static CommitHookHandlerError fromErrorCollection(String command, String issueKey, ErrorCollection errors) {
        CommitHookHandlerError e = new CommitHookHandlerError();
        e.addErrors(command, issueKey, errors);
        return e;
    }

    public void addError(String command, String issueKey, String message) {
        errors.add(new CommitHookError(command, issueKey, message));
    }

    public void addErrors(CommitHookHandlerError other) {
        errors.addAll(other.errors);
    }

    public void addErrors(String command, String issueKey, ErrorCollection other) {

        for(Map.Entry<String, String> error : other.getErrors().entrySet()) {
            addError(command, issueKey, error.getKey() + ": " + error.getValue());
        }

        for(String error : other.getErrorMessages()) {
            addError(command, issueKey, error);
        }
    }

    public List<CommitHookError> getErrors() {
        return errors;
    }

    public boolean isEmpty() {
        return errors.isEmpty();
    }
    
    @Override
    public String toString()
    {
    	return "Errors: " + errors;
    }

    public static class CommitHookError {
        String command;
        String issueKey;
        String message;

        public CommitHookError(String command, String issueKey, String message) {
            this.command = command;
            this.issueKey = issueKey;
            this.message = message;
        }

        public CommitHookError() {

        }

        public String getCommand() {
            return command;
        }

        public String getIssueKey() {
            return issueKey;
        }

        public String getMessage() {
            return message;
        }
    }
}