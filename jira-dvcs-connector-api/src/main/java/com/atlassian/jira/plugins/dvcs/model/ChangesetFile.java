package com.atlassian.jira.plugins.dvcs.model;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import static org.apache.commons.lang.builder.ToStringStyle.SHORT_PREFIX_STYLE;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ChangesetFile
{
    private final ChangesetFileAction fileAction;
    private final String file;

    public ChangesetFile(ChangesetFileAction fileAction, String file)
    {
        this.fileAction = fileAction;
        this.file = file;
    }

    @JsonProperty
    public ChangesetFileAction getFileAction()
    {
        return fileAction;
    }

    @JsonProperty
    public String getFile()
    {
        return file;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ChangesetFile that = (ChangesetFile) o;

        return new EqualsBuilder()
                .append(fileAction, that.fileAction)
                .append(file, that.file)
                .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder()
                .append(fileAction)
                .append(file)
                .hashCode();
    }

    @Override
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }

    @JsonCreator
    private static ChangesetFile fromJSON(@JsonProperty("fileAction") ChangesetFileAction fileAction, @JsonProperty("file") String file)
    {
        return new ChangesetFile(fileAction, file);
    }
}

