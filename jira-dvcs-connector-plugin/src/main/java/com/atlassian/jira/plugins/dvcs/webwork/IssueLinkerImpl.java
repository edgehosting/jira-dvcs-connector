package com.atlassian.jira.plugins.dvcs.webwork;

import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.templaterenderer.annotations.HtmlSafe;
import com.opensymphony.util.TextUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

@Component
public class IssueLinkerImpl implements IssueLinker
{
    // Copied from {@link RendererUtils} (applinks-linker plugin)
    public static final String PRE_LINK_PATTERN_STRING = "(?<![&=\\?>^!~/\\.\\[])\\b";// end if a blank or the end of line is found
    public static final String POST_LINK_PATTERN_STRING = "\\b";// end if a blank or the end of line is found
    public static final String LINK_JIRA_PATTERN_STRING = PRE_LINK_PATTERN_STRING + "(\\p{Lu}{2,}-\\p{Digit}+)" + POST_LINK_PATTERN_STRING;
    
    private static final String ISSUE_URL_PATTERN = "{0}/browse/{1}";
    private static final String ISSUE_LINK_PATTERN = "<a href=\"{0}\">{1}</a>";

    private final ApplicationProperties applicationProperties;

    @Autowired
    public IssueLinkerImpl(@ComponentImport ApplicationProperties applicationProperties)
    {
        this.applicationProperties = checkNotNull(applicationProperties);
    }

    /**
     * Code copied mostly from {@link AbstractAppLinkRendererComponent#linkText} (applinks-linker plugin)
     * @param text
     * @return
     */
    @Override
    @HtmlSafe
    public String createLinks(String text)
    {
        text = TextUtils.htmlEncode(text);
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
