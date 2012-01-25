
function deleteRepository(repositoryId, repositoryUrl) {
    var answer = confirm("Are you sure you want to remove this repository? \n " + repositoryUrl )
    if (answer){
    	AJS.$.ajax({
    	    url: BASE_URL+"/rest/bitbucket/1.0/repository/"+repositoryId,
    	    type: 'DELETE',
    	    success: function(result) {
    	    	window.location.reload();    	    }
    	});
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
    		var syncStatusDiv = AJS.$('.gh_messages.repository'+repo.id+" .content"); 
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
	retrieveSyncStatus();
}

function submitFunction(a){
    var repositoryUrl = AJS.$("#url").val();
    var privateBbCredentialsVisible = AJS.$("#privateBbCredentialsVisible").val();
    //var atlToken = AJS.$("#atl_token").val();

    var requestUrl = BASE_URL+"/rest/bitbucket/1.0/urlinfo?repositoryUrl=" + encodeURIComponent(repositoryUrl)

    AJS.$("#aui-message-bar").empty();
	AJS.messages.generic({
	    title: "Working...",
	    body: "Trying to connect to the repository."
	});

	AJS.$.getJSON(requestUrl, function(data) {
        AJS.$("#isPrivate").val(data.isPrivate);

        if (data.repositoryType == "github")
    		AJS.$("#repoEntry").attr("action", BASE_URL+"/secure/admin/AddGithubRepository.jspa");
        else if (data.repositoryType == "bitbucket")
    		AJS.$("#repoEntry").attr("action", BASE_URL+"/secure/admin/AddBitbucketRepository.jspa");

        // todo: ak BB nema oauth
        if (privateBbCredentialsVisible == "false" && data.repositoryType == "bitbucket" && data.isPrivate) {
            AJS.$("#privateBbCredentialsVisible").val("true");
            AJS.$("#aui-message-bar").empty();

            var credentialsHtml = "<h3>For private Bitbucket repository you have to add access credentials</h3>" +
                "<div class='field-group'>" +
                "<label for='bbUsername'>Username</label>" +
                "<input type='text' name='bbUsername' id='bbUsername' value=''></div>" +
                "<div class='field-group'>" +
                "<label for='bbPassword'>Password</label>" +
                "<input type='password' name='bbPassword' id='bbPassword' value=''></div>";
            AJS.$("#bbCredentials").html(credentialsHtml);
        } else {
            AJS.$('#repoEntry').submit();
        }

//        if (data.repositoryType == "github")
//            var addRepositoryUrl = BASE_URL+"/secure/admin/AddGithubRepository.jspa?atl_token=" + atlToken;
//        else if (data.repositoryType == "bitbucket")
//            var addRepositoryUrl = BASE_URL+"/secure/admin/AddBitbucketRepository!default.jspa";
//        AJS.$.ajax({
//            type: "post",
//            url: addRepositoryUrl,
//            data: {},
//            success:function(response) {
//                var addRepositoryContent = AJS.$('#addRepositoryContent');
//                addRepositoryContent.html(response);
//        },
//        error: function(response) {
//            alert("failed");
//        }});

    }).error(function(a){
        AJS.$("#aui-message-bar").empty();
    	AJS.messages.error({
    	    title: "Error!",
    	    body: "The repository url [<b>"+AJS.$("#url").val()+"</b>] is incorrect or the repository is not responding."
    	});
    });
    return false;
}

function showAddRepoDetails(show)
{
    if (show)
    {
    	AJS.$('#linkRepositoryButton').fadeOut(function(){
    		AJS.$('#addRepositoryDetails').slideDown();
    	});
    } else {
        AJS.$('#addRepositoryDetails').slideUp(function(){
        	AJS.$('#linkRepositoryButton').fadeIn();
        	AJS.$("#bbCredentials").html("");
        });
    }

}

AJS.$(document).ready(function(){
	if(typeof init_repositories == 'function')
	{
		init_repositories();
	}
	
//	jQuery('#footer').addClass('footer');
	
})


