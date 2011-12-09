package com.atlassian.jira.plugins.bitbucket;

import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import com.atlassian.sal.api.ApplicationProperties;


public class IssueLinkerImpl implements IssueLinker
{
    // Copied from {@link RendererUtils} (applinks-linker plugin)
    public static final String PRE_LINK_PATTERN_STRING = "(?<![&=\\?>^!~/\\.\\[])\\b";// end if a blank or the end of line is found
    public static final String POST_LINK_PATTERN_STRING = "\\b";// end if a blank or the end of line is found
    public static final String LINK_JIRA_PATTERN_STRING = PRE_LINK_PATTERN_STRING + "(\\p{Lu}{2,}-\\p{Digit}+)" + POST_LINK_PATTERN_STRING;
    
    private static final String ISSUE_URL_PATTERN = "{0}/browse/{1}";
    private static final String ISSUE_LINK_PATTERN = "<a href=\"{0}\">{1}</a>";

    private final ApplicationProperties applicationProperties;
    

    public IssueLinkerImpl(ApplicationProperties applicationProperties)
    {
        this.applicationProperties = applicationProperties;
    }

    /**
     * Code copied mostly from {@link AbstractAppLinkRendererComponent#linkText} (applinks-linker plugin)
     * @param text
     * @return
     */
    @Override
    public String createLinks(String text)
    {
        String baseUrl = applicationProperties.getBaseUrl();
        
        if (StringUtils.isBlank(text)) return "";
        StringBuffer buff = new StringBuffer();
        
        Matcher matcher;
        while ((matcher = Pattern.compile(LINK_JIRA_PATTERN_STRING).matcher(text)).find())
        {
            buff.append(text.substring(0, matcher.start(1)));

            String key = matcher.group(1);
            String url = MessageFormat.format(ISSUE_URL_PATTERN, baseUrl,key);
            String aLink = MessageFormat.format(ISSUE_LINK_PATTERN, url, key);
            
            buff.append(aLink);
            text = text.substring(matcher.end(1));
        }

        // append any remaining body (or the whole thing if no matches occurred)
        buff.append(text);
        return buff.toString();    
   }

}
