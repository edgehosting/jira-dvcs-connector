package com.atlassian.jira.plugins.dvcs.smartcommits.handlers;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.IssueInputParametersImpl;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.plugins.dvcs.smartcommits.CommandType;
import com.atlassian.jira.plugins.dvcs.smartcommits.model.CommitHookHandlerError;
import com.atlassian.jira.plugins.dvcs.smartcommits.model.Either;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.workflow.WorkflowManager;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.opensymphony.workflow.loader.ActionDescriptor;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

@ExportAsService (CommandHandler.class)
@Component ("smartcommitsTransitionsHandler")
public class TransitionHandler implements CommandHandler<Issue>
{

    private static CommandType CMD_TYPE = CommandType.TRANSITION;

    // private Pattern IN_TRANSITION_PATTERN = Pattern.compile("(#\\S*)(.*)");

    private static final String NO_STATUS = "fisheye.commithooks.transition.unknownstatus";

    private static final String NO_COMMAND_PROVIDED_TEMPLATE = "fisheye.commithooks.transition.nocommand";
    private static final String NO_ALLOWED_ACTIONS_TEMPLATE = "fisheye.commithooks.transition.noactions";
    private static final String NO_MATCHING_ACTIONS_TEMPLATE = "fisheye.commithooks.transition.nomatch";
    private static final String MULTIPLE_ACTIONS_TEMPLATE = "fisheye.commithooks.transition.ambiguous";

    private final IssueService issueService;
    private final WorkflowManager workflowManager;
    private final JiraAuthenticationContext jiraAuthenticationContext;

    @Autowired
    public TransitionHandler(@ComponentImport IssueService issueService,
            @ComponentImport WorkflowManager workflowManager,
            @ComponentImport JiraAuthenticationContext jiraAuthenticationContext)
    {
        this.issueService = issueService;
        this.workflowManager = workflowManager;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
    }

    @Override
	public CommandType getCommandType() {
        return CMD_TYPE;
    }

    @Override
	public Either<CommitHookHandlerError, Issue> handle(User user, MutableIssue issue, String commandName, List<String> args, Date commitDate) {
        
    	String cmd = commandName;
        final I18nHelper i18nHelper = jiraAuthenticationContext.getI18nHelper();
    	
    	String comment = (args != null && args.size() == 1) ? args.get(0) : null;
      
        if (cmd == null || cmd.equals("")) {
            return Either.error(CommitHookHandlerError.fromSingleError(CMD_TYPE.getName(), issue.getKey(),
                    i18nHelper.getText(NO_COMMAND_PROVIDED_TEMPLATE, issue.getKey())));
        }

        Collection<ActionDescriptor> actions = getActionsForIssue(issue);

        Collection<ValidatedAction> validActions =
                getValidActions(actions, user, issue, new IssueInputParametersImpl(), comment);

        if (validActions.isEmpty()) {
            return Either.error(CommitHookHandlerError.fromSingleError(CMD_TYPE.getName(), issue.getKey(),
                    i18nHelper.getText(NO_ALLOWED_ACTIONS_TEMPLATE, issue.getKey())));
        }

        Collection<ValidatedAction> matchingValidActions = getMatchingActionsForCommand(cmd, validActions);

        if (matchingValidActions.isEmpty()) {

            String validActionNames = StringUtils.join(getActionNamesIterator(validActions), ", ");

            return Either.error(CommitHookHandlerError.fromSingleError(CMD_TYPE.getName(), issue.getKey(),
                    i18nHelper.getText(NO_MATCHING_ACTIONS_TEMPLATE, issue.getKey(), getIssueState(issue), cmd, validActionNames)));

        } else if (matchingValidActions.size() > 1) {

            String validActionNames = StringUtils.join(getActionNamesIterator(matchingValidActions), ", ");

            return Either.error(CommitHookHandlerError.fromSingleError(CMD_TYPE.getName(), issue.getKey(),
                    i18nHelper.getText(MULTIPLE_ACTIONS_TEMPLATE, cmd, issue.getKey(), getIssueState(issue), validActionNames)));
        } else {
            
            IssueService.TransitionValidationResult validation = matchingValidActions.iterator().next().validation;
            IssueService.IssueResult result = issueService.transition(user, validation);
            if (!result.isValid()) {
                return Either.error(CommitHookHandlerError.fromErrorCollection(
                        CMD_TYPE.getName(), issue.getKey(), result.getErrorCollection()));
            }

            return Either.value((Issue)result.getIssue());
        }
    }

    private String getIssueState(Issue issue) {
        Status s = issue.getStatusObject();
        final I18nHelper i18nHelper = jiraAuthenticationContext.getI18nHelper();
        return s == null ? i18nHelper.getText(NO_STATUS) : s.getName();
    }

    private Iterator<String> getActionNamesIterator(Collection<ValidatedAction> matchingValidActions) {
        return Iterables.transform(matchingValidActions,
                    new Function<ValidatedAction, String>() {
                        @Override
						public String apply(ValidatedAction in) {
                            return in.action.getName();
                        }
                    }).iterator();
    }

    private Collection<ActionDescriptor> getActionsForIssue(MutableIssue issue) {
        return workflowManager.getWorkflow(issue).getAllActions();
    }

    private Collection<ValidatedAction> getMatchingActionsForCommand(String cmd, Collection<ValidatedAction> actions) {
        String cmdSanitized = cmd.trim().toLowerCase(Locale.US);
        String cmdWithSpaces = cmdSanitized.replace('-', ' ');

        Collection<ValidatedAction> firstShotActions = new ArrayList<ValidatedAction>();
        Collection<ValidatedAction> secondShotActions = new ArrayList<ValidatedAction>();
        for (ValidatedAction validatedAction : actions) {
            String name = validatedAction.action.getName().toLowerCase(Locale.US);

            if (name.equals(cmdSanitized)) { // choose an exact match immediately
                return Arrays.asList(validatedAction);

            } else if (name.startsWith(cmdSanitized)) {
                firstShotActions.add(validatedAction);

            } else if (name.startsWith(cmdWithSpaces)) {
                secondShotActions.add(validatedAction);
            }
        }

        return firstShotActions.isEmpty() ? secondShotActions : firstShotActions;
    }

    private class ValidatedAction {
        ActionDescriptor action;
        IssueService.TransitionValidationResult validation;

        public ValidatedAction(ActionDescriptor action, IssueService.TransitionValidationResult validation) {
            this.action = action;
            this.validation = validation;
        }
    }

    private Collection<ValidatedAction> getValidActions(
            Collection<ActionDescriptor> actionsToValidate,
            User user,
            MutableIssue issue,
            IssueInputParameters parameters,
            String comment) {

        Collection<ValidatedAction> validations =
            new ArrayList<ValidatedAction>();
        for (ActionDescriptor ad : actionsToValidate) {
            IssueInputParametersImpl input = new IssueInputParametersImpl();
            if (comment != null) {
                input.setComment(comment);
            }
            IssueService.TransitionValidationResult validation =
                    issueService.validateTransition(user, issue.getId(), ad.getId(), input);
            if (validation.isValid()) {
                validations.add(new ValidatedAction(ad, validation));
            }
        }
        return validations;
    }
    

}
