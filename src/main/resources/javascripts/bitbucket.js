
function confirmation(delete_url) {
    var answer = confirm("Are you sure you want to remove this repository?")
    if (answer){
        window.location = delete_url;
    }
}

function toggleMoreFiles(target_div){
        AJS.$('#' + target_div).toggle();
        AJS.$('#see_more_' + target_div).toggle();
        AJS.$('#hide_more_' + target_div).toggle();
}


function retrieveSyncStatus() {
    AJS.$.getJSON(BASE_URL+"/rest/bitbucket/1.0/repositories", function (data) {
    	AJS.$.each(data.repositories, function(a,repo){
    		var syncStatusDiv = AJS.$('.gh_messages.repository'+repo.id); 
    		var syncIconElement = AJS.$('.syncicon.repository'+repo.id);
    		var syncHtml;
    		var syncIcon;
    		if (repo.sync)
    		{
    			if(repo.sync.isFinished)
				{
    				syncIcon = "finished";
    				syncHtml = "<strong>Sync Finished:</strong>"
				} else
				{
    				syncIcon = "running";
					syncHtml = "<strong>Sync Running:</strong>"
				}
    			syncHtml = syncHtml + " Synchronized <strong>"+repo.sync.changesetCount+"</strong> changesets, found <strong>"+repo.sync.jiraCount+"</strong> matching JIRA issues"; 
    			if (repo.sync.error)
    			{
    				syncIcon = "error";
    				syncHtml = syncHtml + "<div class=\"error\"><strong>Sync Failed:</strong> "+repo.sync.error+"</div>";
    			} 
    		} else
			{
				syncIcon = "";
    			syncHtml = "No information about sync available"
			}
    		syncIconElement.removeClass("finished").removeClass("running").removeClass("error").addClass(syncIcon);
    		syncStatusDiv.html(syncHtml);
    		
    	});
    	window.setTimeout(retrieveSyncStatus, 2000)
    })
}

function forceSync(repositoryId){
	AJS.$.post(BASE_URL+"/rest/bitbucket/1.0/repository/"+repositoryId+"/sync");
}

AJS.$(document).ready(function(){
	if(init_repositories)
	{
		init_repositories();
	}
})


