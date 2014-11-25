package com.atlassian.jira.plugins.dvcs.activeobjects;

import net.java.ao.RawEntity;
import net.java.ao.atlassian.AtlassianTableNameConverter;
import net.java.ao.atlassian.TablePrefix;
import net.java.ao.schema.TableNameConverter;

/**
 * AO {@link net.java.ao.schema.TableNameConverter} that hard codes the DVCS prefix for testing
 */
public class DvcsConnectorTableNameConverter implements TableNameConverter
{
    private final TableNameConverter delegate;

    public DvcsConnectorTableNameConverter()
    {
        delegate = new AtlassianTableNameConverter(new TestPrefix());
    }

    @Override
    public String getName(Class<? extends RawEntity<?>> clazz)
    {
        return delegate.getName(clazz);
    }

    public static final class TestPrefix implements TablePrefix
    {
        public String prepend(String string)
        {
            return new StringBuilder().append("AO_E8B6CC_").append(string).toString();
        }
    }
}
