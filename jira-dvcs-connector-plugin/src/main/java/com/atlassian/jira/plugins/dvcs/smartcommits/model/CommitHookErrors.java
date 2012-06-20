package com.atlassian.jira.plugins.dvcs.smartcommits.model;

import com.atlassian.jira.util.ErrorCollection;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class CommitHookErrors {
    @XmlElementWrapper(name = "errors")
    @XmlElement(name = "error")
    List<CommitHookError> errors;

    public CommitHookErrors() {
        errors = new ArrayList<CommitHookError>();
    }

    public static CommitHookErrors fromSingleError(String command, String issueKey, String message) {
        CommitHookErrors e = new CommitHookErrors();
        e.addError(command, issueKey, message);
        return e;
    }

    public static CommitHookErrors fromErrorCollection(String command, String issueKey, ErrorCollection errors) {
        CommitHookErrors e = new CommitHookErrors();
        e.addErrors(command, issueKey, errors);
        return e;
    }

    public void addError(String command, String issueKey, String message) {
        errors.add(new CommitHookError(command, issueKey, message));
    }

    public void addErrors(CommitHookErrors other) {
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

    @XmlAccessorType(XmlAccessType.FIELD)
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