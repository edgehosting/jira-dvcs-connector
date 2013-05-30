function showChangesetByFork(node, repoId, repoName, repoUrl) {


    // hide all changesets CommitPanels
    AJS.$('#' + node + ' .CommitPanel').each(function(n, el) {
        $(el).hide();
    });

    // show only that which is from given repository
    AJS.$('#' + node + "-" + repoId).show();
    
    registerForksTooltip(node, repoName, repoUrl);
}

function registerForksTooltip(node, repoName, repoUrl) {
	registerInlineDialogTooltip(AJS.$("#fork-drop-down-"+node), "This changeset is present in multiple repositories (forks). Currently showing <a href='"+repoUrl+"' target='_blank'>" + repoName + "</a>.");
}

function registerInlineDialogTooltip(element, body) {
    return AJS.InlineDialog(AJS.$(element), "tooltip_"+AJS.$(element).attr('id'),
            function(content, trigger, showPopup) {
                content.css({"padding":"10px"}).html(body);
                showPopup();
                return false;
            },
            {onHover:true, hideDelay:200, showDelay:1000, arrowOffsetX:-8, offsetX:-80, addActiveClass:false}
        );
}
