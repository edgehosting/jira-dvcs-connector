package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Martin Skurla
 */
public class URLPathFormatter
{
    private URLPathFormatter() {}


    public static String format(String formattingString, String... arguments)
    {
        List<String> encodedArguments = new ArrayList<String>(arguments.length);

        for (String argument : arguments)
        {
            try
            {
                encodedArguments.add(URLEncoder.encode(argument, "UTF-8"));
            } catch (UnsupportedEncodingException ex)
            {
                throw new AssertionError(ex); // should never happen
            }
        }

        return String.format(formattingString, encodedArguments.toArray());
    }
}
