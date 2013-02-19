package com.atlassian.jira.plugins.dvcs.activity;

import net.java.ao.Preload;
import net.java.ao.schema.Table;

@Preload
@Table("PR_LINE_COMMENT")
public interface RepositoryActivityPullRequestLineCommentMapping extends RepositoryActivityPullRequestCommentMapping
{
    String FILE = "FILE";

    //
    // getters
    //

    String getFile();
    
    //
    // setters
    //
    
    void setFile(String file);
}
