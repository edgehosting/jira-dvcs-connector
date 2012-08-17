package com.atlassian.jira.plugins.dvcs.dao.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Martin Skurla mskurla@atlassian.com
 */
@RunWith(Parameterized.class)
public class MapRemovingNullCharacterFromStringValuesTest
{
    private final String inputString;
    private final String expectedTransformedString;   


    public MapRemovingNullCharacterFromStringValuesTest(String inputString, String expectedTransformedString)
    {
        this.inputString = inputString;
        this.expectedTransformedString = expectedTransformedString;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> values()
    {
        Object[][] data = new Object[][]
        {
            {"te\u0000xt",        "text"},
            {"te\u0000\u0000xt",  "text"},
            {"te\u0000 \u0000xt", "te xt"}
        };       

        return Arrays.asList(data);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void removingVariousNullCharacterOccurences_ShouldWorkCorrectly()
    {
        Map<String, String> map = (Map<String, String>) (Map<?,?>) new MapRemovingNullCharacterFromStringValues();
        map.put("key", inputString);       

        assertThat(map.get("key"), is(expectedTransformedString));
    }
}
