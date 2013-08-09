if (typeof(com) == 'undefined') com = {};
if (typeof(com.atlassian) == 'undefined') com.atlassian = {};
if (typeof(com.atlassian.jira) == 'undefined') com.atlassian.jira = {};
if (typeof(com.atlassian.jira.dvcs) == 'undefined') com.atlassian.jira.dvcs = {};

com.atlassian.jira.dvcs.showChangesetByFork = function(node, repoId, repoName, repoUrl) {

    // hide all changesets CommitPanels
    AJS.$('#' + node + ' .CommitPanel').each(function(n, el) {
        $(el).hide();
    });

    // show only that which is from given repository
    AJS.$('#' + node + '-' + repoId).show();
    
    registerForksTooltip(node, repoName, repoUrl);
};

com.atlassian.jira.dvcs.registerForksTooltip = function(node, repoName, repoUrl) {
    com.atlassian.jira.dvcs.registerInlineDialogTooltip(AJS.$('#fork-drop-down-' + node), "This changeset is present in multiple repositories (forks). Currently showing <a href='"+repoUrl+"' target='_blank'>" + repoName + "</a>.");
};

com.atlassian.jira.dvcs.registerInlineDialogTooltip = function (element, body) {
    AJS.InlineDialog(AJS.$(element), 'tooltip_' + AJS.$(element).attr('id'),
            function(content, trigger, showPopup) {
                content.css({'padding' : '10px'}).html(body);
                showPopup();
                return false;
            },
            {onHover : true, hideDelay : 200, showDelay : 1000, arrowOffsetX : -8, offsetX : -80, addActiveClass : false}
        );
};
