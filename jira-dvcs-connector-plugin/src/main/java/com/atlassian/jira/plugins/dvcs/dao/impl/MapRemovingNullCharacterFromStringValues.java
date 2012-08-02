package com.atlassian.jira.plugins.dvcs.dao.impl;

import java.util.HashMap;

/**
 * MapRemovingNullCharacter
 *
 * @author Martin Skurla mskurla@atlassian.com
 */
public final class MapRemovingNullCharacterFromStringValues extends HashMap<String, Object>
{
    private static final String NULL_CHARACTER = "\u0000";

    @Override
    public Object put(String key, Object value)
    {
        Object transformedValue = value instanceof String ? removeNullCharacters((String) value)
                                                          : value;
        return super.put(key, transformedValue);
    }
    
    private String removeNullCharacters(String inputText)
    {
        return inputText.contains(NULL_CHARACTER) ? inputText.replace(NULL_CHARACTER, "")
                                                  : inputText;
    }
}
