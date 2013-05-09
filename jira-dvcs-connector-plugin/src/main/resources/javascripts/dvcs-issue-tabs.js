function showChangesetByFork(node, repo) {


    // hide all changesets CommitPanels
    AJS.$('#' + node + ' .CommitPanel').each(function(n, el) {
        $(el).hide();
    });

    // show only that which is from given repository
    AJS.$('#' + node + "-" + repo).show();
}

