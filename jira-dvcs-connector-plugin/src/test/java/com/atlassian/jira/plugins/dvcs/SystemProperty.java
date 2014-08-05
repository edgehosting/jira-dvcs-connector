package com.atlassian.jira.plugins.dvcs;

/**
 * Helper for setting/unsetting system properties in tests.
 */
public class SystemProperty
{
    private final String name;
    private final String previous;

    public static SystemProperty set(String name, Object value)
    {
        return new SystemProperty(name, value);
    }

    public SystemProperty(String name, Object value)
    {
        this.name = name;
        this.previous = nullSafeSet(name, value);
    }

    /**
     * Restores the original value.
     */
    public void restore()
    {
        nullSafeSet(name, previous);
    }

    private static String nullSafeSet(String name, Object value)
    {
        return value != null ? System.setProperty(name, String.valueOf(value)) : System.clearProperty(name);
    }
}
