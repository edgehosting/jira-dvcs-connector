package com.atlassian.jira.plugins.dvcs.dao.impl;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Map;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * @author Martin Skurla mskurla@atlassian.com
 */
public class MapRemovingNullCharacterFromStringValuesTest
{
    @DataProvider
    private Object[][] textsWithNullCharacterDataProvider()
    {
        return new Object[][]
        {
            {"te\u0000xt",        "text"},
            {"te\u0000\u0000xt",  "text"},
            {"te\u0000 \u0000xt", "te xt"}
        };
    }

    
    @Test(dataProvider="textsWithNullCharacterDataProvider")
    public void removingVariousNullCharacterOccurences_ShouldWorkCorrectly(String inputString,
        String expectedTransformedString)
    {
        Map<String, Object> map = new MapRemovingNullCharacterFromStringValues();
        map.put("key", inputString);
        
        assertThat(map.get("key")).isEqualTo(expectedTransformedString);
    }
}
