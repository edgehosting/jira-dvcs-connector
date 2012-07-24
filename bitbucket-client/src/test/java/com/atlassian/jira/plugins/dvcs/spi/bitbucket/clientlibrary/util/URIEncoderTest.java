package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.util;


import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.fest.assertions.api.Assertions.*;


/**
 * @author Martin Skurla
 */
public class URIEncoderTest
{
    
    @DataProvider
    private static Object[][] dataProviderForParameterEncoding()
    {
        return new Object[][] {
            { mapFromKeyValuePairs("a", "1 2"),             "a=1+2" },
            { mapFromKeyValuePairs("b", "3/4"),             "b=3%2F4" },
            { mapFromKeyValuePairs("a", "1 2", "b", "3/4"), "a=1+2&b=3%2F4" }
        };
    }
    
    @DataProvider
    private static Object[][] dataProviderForUriEncoding()
    {
        return new Object[][] {
            { "https://fake",       Collections.emptyMap(),                   "https://fake" },
            { "https://fake space", Collections.emptyMap(),                   "https://fake%20space" },
            { "https://fake",       mapFromKeyValuePairs("a", "1"),           "https://fake?a=1" },
            { "https://fake",       mapFromKeyValuePairs("a", "1", "b", "2"), "https://fake?a=1&b=2" },
            { "https://fake space", mapFromKeyValuePairs("a", "1", "b", "2"), "https://fake%20space?a=1&b=2"
                    }
        };
    }
    

    @Test(dataProvider="dataProviderForParameterEncoding")
    public void encodingHttpParameters_ShouldEncodeSpecialCharacters(Map<String, String> parameters,
            String expectedEncodedParameters)
    {
        String encodedParameters = URIEncoder.encodeHttpParameters(parameters, URIEncoder.UTF_8_ENCODING);
        
        assertThat(encodedParameters).isEqualTo(expectedEncodedParameters);
    }
    
    @Test(dataProvider="dataProviderForUriEncoding")
    public void encodingUri_ShouldEncodeBothUriAndParameters(String uriPath, Map<String, String> parameters,
            String expectedEncodedUri)
    {
        String encodedUri = URIEncoder.encodeURI(uriPath, parameters, URIEncoder.UTF_8_ENCODING);
        
        assertThat(encodedUri).isEqualTo(expectedEncodedUri);
    }
    
    
    private static Map<String, String> mapFromKeyValuePairs(String... keysAndValues)
    {
        Map<String, String> map = new LinkedHashMap<String, String>(); // needed because order in assertions matters
        
        for (int index = 0; index < keysAndValues.length; )
        {
            map.put(keysAndValues[index], keysAndValues[index + 1]);
            index += 2;
        }
        
        return map;
    }
}
