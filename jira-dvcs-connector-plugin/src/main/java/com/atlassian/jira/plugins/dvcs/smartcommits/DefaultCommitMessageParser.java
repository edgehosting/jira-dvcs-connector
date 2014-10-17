package com.atlassian.jira.plugins.dvcs.smartcommits;

import com.atlassian.jira.plugins.dvcs.smartcommits.model.CommitCommands;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Originally from com.atlassian.jirafisheyeplugin.notifications.CommitCommentParser
 *
 * @author jhinch
 */
@ExportAsService (CommitMessageParser.class)
@Component
public class DefaultCommitMessageParser implements CommitMessageParser
{

    public static final Pattern JIRA_ISSUE_PATTERN = Pattern.compile("(?<![&=\\?>^!~/\\-])\\b(\\p{Lu}[\\p{Lu}\\p{Digit}]+-\\p{Digit}+)\\b");
    public static final Pattern COMMAND_PATTERN = Pattern.compile("(?:^|\\s)#([A-Za-z][A-Za-z\\-]*)");

    /**
     * This is what we are parsing:
     * <pre>
     *     .-----------------------------.
     *     v                             |
     * >>----| svn-commit-message-line |-+--><
     *
     * svn-commit-message-line:
     * |---| non-issue-key-text |-+---------------------------+--newline--|
     *                            |  .---------------------.  |
     *                            |  v                     |  |
     *                            '----| trigger-message |-+--'
     *
     * non-issue-key-text:
     * Any text that doesn't contain an issue key.
     *
     * trigger-message:
     *     .-------------.
     *     v             |
     * |-----<issue-key>-+--+------------------+---+-------------------+----|
     *                      '-| ignored-text |-'   | .---------------. |
     *                                             | v               | |
     *                                             '----| command |--+-+
     *
     * ignored-text:
     * Any text that doesn't contain an issue key, or a # followed by a command name.
     *
     * command:
     * |--hash---<command>--+-----------------+----|
     *                      '--| arguments |--'
     *
     * arguments:
     * Any text that doesn't contain an issue key, or a # followed by a comamnd name
     *
     * </pre>
     *
     * @param comment The comment message to parse
     * @return The parsed actions
     */
    @Override
    public CommitCommands parseCommitComment(final String comment)
    {
        // Split the comment into lines
        final String[] lines = comment.split("\r?\n");

        final List<CommitCommands.CommitCommand> commands = new ArrayList<CommitCommands.CommitCommand>();
        final CommitCommands results = new CommitCommands(commands);

        for (final String line : lines)
        {
            final Matcher issueKeyMatcher = JIRA_ISSUE_PATTERN.matcher(line);
            int pos = 0;
            while (pos < line.length())
            {
                // Find the first occurance of an issue key
                if (issueKeyMatcher.find(pos))
                {
                    final List<String> issueKeys = new ArrayList<String>();
                    issueKeys.add(issueKeyMatcher.group(1));
                    pos = issueKeyMatcher.end(1);
                    int end = line.length();
                    // See if more issue keys exist after this one, with only white space or commas between them
                    while (issueKeyMatcher.find())
                    {
                        if (line.substring(pos, issueKeyMatcher.start(1)).matches(",?\\s*"))
                        {
                            issueKeys.add(issueKeyMatcher.group(1));
                            pos = issueKeyMatcher.end(1);
                        }
                        else
                        {
                            // There's something other than a comma and spaces between these two issues, this is where
                            // all the commands end for the previous issues, so store it
                            end = issueKeyMatcher.start();
                            break;
                        }
                    }
                    // Our commands exist between the end of the last issue key, and the end of either the line,
                    // or the start of the next issue key if one was found, so set the region on the matcher
                    final Matcher commandMatcher = COMMAND_PATTERN.matcher(line.substring(pos, end));
                    if (commandMatcher.find())
                    {
                        // Found a command, but need to find the next one to know where its arguments end
                        String command = commandMatcher.group(1);
                        int commandStart = pos + commandMatcher.end(1);
                        while (commandMatcher.find())
                        {
                            // We now know where the arguments end, create it, and store the next command
                            commands.addAll(createCommandsForIssueKeys(issueKeys, command, line.substring(commandStart,
                                    pos + commandMatcher.start()).trim()));
                            command = commandMatcher.group(1);
                            commandStart = pos + commandMatcher.end(1);
                        }
                        commands.addAll(createCommandsForIssueKeys(issueKeys, command, line.substring(commandStart,
                                end).trim()));
                    }
                    // Update the position
                    pos = end;
                }
                else
                {
                    // No more patterns
                    pos = line.length();
                }
            }
        }
        return results;
    }

    private List<CommitCommands.CommitCommand> createCommandsForIssueKeys(List<String> issueKeys, String command, String arguments)
    {
        List<CommitCommands.CommitCommand> commands = new ArrayList<CommitCommands.CommitCommand>();
        for (String issueKey : issueKeys)
        {
            commands.add(new CommitCommands.CommitCommand(issueKey, command, Arrays.asList(arguments)));
        }
        return commands;
    }
}
