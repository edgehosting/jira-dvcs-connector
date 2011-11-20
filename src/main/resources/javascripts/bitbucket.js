
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
    	window.setTimeout(retrieveSyncStatus, 4000)
    })
}

function forceSync(repositoryId){
	AJS.$.post(BASE_URL+"/rest/bitbucket/1.0/repository/"+repositoryId+"/sync");
}

function submitFunction(a){
    var repositoryUrl = AJS.$("#url").val();
    var requestUrl = BASE_URL+"/rest/bitbucket/1.0/urlinfo?repositoryUrl=" + encodeURIComponent(repositoryUrl)

    AJS.$.getJSON(requestUrl, function(data) {
    	if (data.repositoryType == "github")
    		AJS.$("#repoEntry").attr("action", BASE_URL+"/secure/admin/AddGithubRepository!default.jspa");
    	else if (data.repositoryType == "bitbucket")
    		AJS.$("#repoEntry").attr("action", BASE_URL+"/secure/admin/AddBitbucketRepository!default.jspa");
    	AJS.$("#isPrivate").val(data.isPrivate);
    	AJS.$('#repoEntry').submit();

    }).error(function huh(a){
    	console.log("yah")
    });
    return false;
}

AJS.$(document).ready(function(){
	if(typeof init_repositories == 'function')
	{
		init_repositories();
	}
})


