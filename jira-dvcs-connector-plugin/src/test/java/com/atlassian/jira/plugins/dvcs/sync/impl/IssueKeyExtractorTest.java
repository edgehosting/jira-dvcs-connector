package com.atlassian.jira.plugins.dvcs.sync.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.fest.assertions.api.Assertions.*;

/**
 * @author Martin Skurla
 */
@RunWith(Parameterized.class)
public class IssueKeyExtractorTest {

    @Parameters
    public static Collection<Object[]> data()
    {
        return Arrays.asList(new Object[][]
            {
                {"ABC-123",                new String[] {"ABC-123"}}, // exactly the key
                {"ABC-123 text",           new String[] {"ABC-123"}}, // starting line with key
                {"message ABC-123",        new String[] {"ABC-123"}}, // ending line with key
                {"message ABC-123 text",   new String[] {"ABC-123"}}, // separated by whitespaces
                {"message\nABC-123\ntext", new String[] {"ABC-123"}}, // separated by newlines
                {"message\rABC-123\rtext", new String[] {"ABC-123"}}, // separated by carriage returns
                {"message.ABC-123.text",   new String[] {"ABC-123"}}, // separated by dots
                {"message:ABC-123:text",   new String[] {"ABC-123"}}, // separated by colons
                {"message,ABC-123,text",   new String[] {"ABC-123"}}, // separated by commas
                {"message;ABC-123;text",   new String[] {"ABC-123"}}, // separated by semicolons
                {"message&ABC-123&text",   new String[] {"ABC-123"}}, // separated by ampersands
                {"message=ABC-123=text",   new String[] {"ABC-123"}}, // separated by equal signs
                {"message?ABC-123?text",   new String[] {"ABC-123"}}, // separated by question marks
                {"message!ABC-123!text",   new String[] {"ABC-123"}}, // separated by exclamation marks
                {"message/ABC-123/text",   new String[] {"ABC-123"}}, // separated by slashes
                {"message\\ABC-123\\text", new String[] {"ABC-123"}}, // separated by back slashes
                {"message~ABC-123~text",   new String[] {"ABC-123"}}, // separated by tildas
                
                {"ABC-123 DEF-456",                 new String[] {"ABC-123", "DEF-456"}}, // exactly the keys
                {"message ABC-123 DEF-456 text",    new String[] {"ABC-123", "DEF-456"}}, // separated by whitespaces
                {"message\nABC-123\nDEF-456\ntext", new String[] {"ABC-123", "DEF-456"}}, // separated by newlines
                {"message\rABC-123\rDEF-456\rtext", new String[] {"ABC-123", "DEF-456"}}, // separated by carriage returns
                {"message.ABC-123.DEF-456.text",    new String[] {"ABC-123", "DEF-456"}}, // separated by dots
                {"message:ABC-123:DEF-456:text",    new String[] {"ABC-123", "DEF-456"}}, // separated by colons
                {"message,ABC-123,DEF-456,text",    new String[] {"ABC-123", "DEF-456"}}, // separated by commas
                {"message;ABC-123;DEF-456;text",    new String[] {"ABC-123", "DEF-456"}}, // separated by semicolons
                {"message&ABC-123&DEF-456&text",    new String[] {"ABC-123", "DEF-456"}}, // separated by ampersands
                {"message=ABC-123=DEF-456=text",    new String[] {"ABC-123", "DEF-456"}}, // separated by equal signs
                {"message?ABC-123?DEF-456?text",    new String[] {"ABC-123", "DEF-456"}}, // separated by question marks
                {"message!ABC-123!DEF-456!text",    new String[] {"ABC-123", "DEF-456"}}, // separated by exclamation marks
                {"message/ABC-123/DEF-456/text",    new String[] {"ABC-123", "DEF-456"}}, // separated by slashes
                {"message\\ABC-123\\DEF-456\\text", new String[] {"ABC-123", "DEF-456"}}, // separated by back slashes
                {"message~ABC-123~DEF-456~text",    new String[] {"ABC-123", "DEF-456"}}, // separated by tildas
                
                {"message without key",          new String[0]},
                {"message ABC-A text",           new String[0]},
                {"message M-123 invalid key",    new String[0]},
                {"message MES- invalid key",     new String[0]},
                {"message -123 invalid key",     new String[0]},
                {"should not parse key0MES-123", new String[0]},
                {"should not parse MES-123key",  new String[0]},
                {"MES-123k invalid char",        new String[0]},
                {"invalid char MES-123k",        new String[0]}
            });
    }
    
    private final String messageToExtract;
    private final String[] expectedExtractedKeys;
    
    
    public IssueKeyExtractorTest(String messageToExtract, String... expectedExtractedKeys)
    {
        this.messageToExtract = messageToExtract;
        this.expectedExtractedKeys = expectedExtractedKeys;
    }
    
    
    @Test
    public void extractorShouldExtractGivenKeyFromGivenMessage()
    {
        Set<String> extractIssueKeys = IssueKeyExtractor.extractIssueKeys(messageToExtract);
        
        if (expectedExtractedKeys.length > 0)
        {
            assertThat(extractIssueKeys).containsOnly(expectedExtractedKeys);
        }
        else
        {
            assertThat(extractIssueKeys).isEmpty();
        }
    }
}
