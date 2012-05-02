function deleteRepository(repositoryId, repositoryUrl) {
    var answer = confirm("Are you sure you want to remove this repository? \n " + repositoryUrl)
    if (answer) {
        AJS.$.ajax({
            url: BASE_URL + "/rest/bitbucket/1.0/repository/" + repositoryId,
            type: 'DELETE',
            success: function(result) {
                window.location.reload();
            }
        });
    }
}

function toggleMoreFiles(target_div) {
    AJS.$('#' + target_div).toggle();
    AJS.$('#see_more_' + target_div).toggle();
    AJS.$('#hide_more_' + target_div).toggle();
}

function retrieveSyncStatus() {
    AJS.$.getJSON(BASE_URL + "/rest/bitbucket/1.0/repositories", function (data) {
        AJS.$.each(data.repositories, function(a, repo) {
            var syncStatusDiv = AJS.$('#sync_status_message_' + repo.id);
            var syncErrorDiv = AJS.$('#sync_error_message_' + repo.id);
            var syncIconElement = AJS.$('#syncicon_' + repo.id);

            var syncStatusHtml = "";
            var syncIcon;

            if (repo.sync) {

                if (repo.sync.isFinished) {
                    if (repo.lastCommitRelativeDate != "") syncIcon = "commits";
                    syncStatusHtml = getLastCommitRelativeDateHtml(repo.lastCommitRelativeDate);

                } else {
                    syncIcon = "running";
                    syncStatusHtml = "Synchronizing: <strong>" + repo.sync.changesetCount + "</strong> changesets, <strong>" + repo.sync.jiraCount + "</strong> issues found";
                    if (repo.sync.synchroErrorCount > 0)
                        syncStatusHtml += ", <span style='color:#e16161;'><strong>" + repo.sync.synchroErrorCount + "</strong> changesets incomplete</span>";

                }
                if (repo.sync.error) {
                    syncStatusHtml = "";
                    syncIcon = "error";
                    syncErrorDiv.html("<div class=\"error\"><strong>Sync Failed:</strong> " + repo.sync.error + "</div>");
                } else {
                	syncErrorDiv.html("");
            	}
            }
            else {
                if (repo.lastCommitRelativeDate != "") syncIcon = "commits";
                syncStatusHtml = getLastCommitRelativeDateHtml(repo.lastCommitRelativeDate);
            }
            syncIconElement.removeClass("commits").removeClass("finished").removeClass("running").removeClass("error").addClass(syncIcon);

            if (syncStatusHtml != "") syncStatusHtml += " <span style='color:#000;'>|</span>";
            syncStatusDiv.html(syncStatusHtml);

        });
        window.setTimeout(retrieveSyncStatus, 4000)
    })
}

function getLastCommitRelativeDateHtml(daysAgo) {
    var html = "";
    if (daysAgo != "") {
        html = "last commit " + daysAgo;
    }
    return html;
}

function forceSync(repositoryId) {
    AJS.$.post(BASE_URL + "/rest/bitbucket/1.0/repository/" + repositoryId + "/sync");
    retrieveSyncStatus();
}

function submitFunction() {

    if (!AJS.$('#projectKey').val()) {
        AJS.$("#aui-message-bar").empty();
        AJS.messages.error({ title: "Error!",
            body: "The project has to be selected. There are no projects configured. Please create a project <a href='" + BASE_URL + "/secure/project/ViewProjects.jspa' target='_blank'>first</a>."
        });
        return false; // project-key has to be selected
    }

    AJS.$('#Submit').attr("disabled", "disabled");
    if (AJS.$('#repoEntry').attr("action")) {
        AJS.messages.hint({ title: "Linking...", body: "Trying to link repository to the project."});
		return true; // submit form
	}

    AJS.$("#aui-message-bar").empty();
    AJS.messages.hint({ title: "Connecting...", body: "Trying to connect to the repository."});

    var repositoryUrl = AJS.$("#url").val().trim();
    var requestUrl = BASE_URL + "/rest/bitbucket/1.0/urlinfo?repositoryUrl=" + encodeURIComponent(repositoryUrl) + "&projectKey="+AJS.$("#projectKey").val();

    AJS.$.getJSON(requestUrl,
        function(data) {
            AJS.$("#aui-message-bar").empty();
            AJS.$("#isPrivate").val(data.isPrivate);

            AJS.$('#Submit').attr("disabled", "");
            if (data.validationErrors.length>0) {
            	AJS.$.each(data.validationErrors, function(i, msg){
            		AJS.messages.error({title : "Error!", body : msg});
            	})
            } else{
            	handler[data.repositoryType].apply(this, arguments);
        	}
    	}).error(function(a) {
            AJS.$("#aui-message-bar").empty();
            AJS.messages.error({ title: "Error!", 
            	body: "The repository url [<b>" + AJS.escapeHtml(AJS.$("#url").val()) + "</b>] is incorrect or the repository is not responding." 
            });
            AJS.$('#Submit').attr("disabled", "");
        });
    return false;
}

	var handler = {
		"bitbucket": function(data){
			AJS.$("#repoEntry").attr("action", BASE_URL + "/secure/admin/AddBitbucketRepository.jspa");

			// hide url input box
			AJS.$('#urlReadOnly').html(AJS.$('#url').val());
			AJS.$('#url').hide(); 
			AJS.$('#urlReadOnly').show();
			
			// hide project selector
			AJS.$('#projectKeyReadOnly').html(AJS.$('#projectKey').val());
	        AJS.$('#projectKey').hide();
			AJS.$('#projectKeyReadOnly').show();
				
			// hide examples
			AJS.$('#examples').hide();

			//show username / password
			var credentialsHtml = ""
				+ "<div class='field-group'>"
				+ "<label for='adminUsername'>Username <span class='notbold'>(requires admin access to repo):</span></label>"
				+ "<input type='text' name='adminUsername' id='adminUsername' value=''></div>"
				+ "<div class='field-group' style='margin-bottom: 10px;'>"
				+ "<label for='adminPassword'>Password</label>"
				+ "<input type='password' name='adminPassword' id='adminPassword' value=''></div>";
			AJS.$("#bbCredentials").html(credentialsHtml);
		}, 
		"github":function(data) {
			AJS.$("#repoEntry").attr("action",BASE_URL + "/secure/admin/AddGithubRepository.jspa");
			AJS.$('#repoEntry').submit();
		}
	}


	
function switchDvcsDetails(selectSwitch) {
	var dvcsType = selectSwitch.selectedIndex;
	if (dvcsType == 0) {
		AJS.$('#repoEntry').attr('action', '/secure/admin/AddBitbucketOrganization.jspa');
		AJS.$('#github-form-section').hide();
		AJS.$('#bitbucket-form-section').fadeIn();
	} else if (dvcsType == 1) {
		AJS.$('#repoEntry').attr('action', '/secure/admin/AddGithubOrganization.jspa');
		AJS.$('#bitbucket-form-section').hide();
		AJS.$('#github-form-section').fadeIn();
	}  
}

//--------------------------------------------------------------------------------------------------
//--------------------------------------------------------------------------------------------------
	

function showAddRepoDetails(show) {

	if (show) {

		// Reset to default view:

		AJS.$('#repoEntry').attr("action", "");
		
		// - hide username/password
		AJS.$("#bitbucket-form-section").hide();

		// - show url field
		AJS.$('#url').show();
		AJS.$('#urlReadOnly').hide();

		//
		AJS.$('#Submit').removeAttr("disabled");

		// show examples
		AJS.$('#examples').show();

		AJS.$('#linkRepositoryButton').fadeOut(function() {
			AJS.$('#addRepositoryDetails').slideDown();
		});

	} else {

		AJS.$('#addRepositoryDetails').slideUp(function() {
			AJS.$('#linkRepositoryButton').fadeIn();
		});

	}
}
function submitFormHandler() {

    AJS.$('#Submit').attr("disabled", "disabled");

    if (AJS.$('#repoEntry').attr("action")) {
        AJS.messages.hint({ title: "Obtaining information...", body: "Trying to obtain repositories information."});
		return true; // submit form
	}

    AJS.$("#aui-message-bar").empty();
    
    AJS.messages.hint({ title: "Identifying...", body: "Trying to identify repository type."});

    var repositoryUrl = AJS.$("#url").val().trim();
    var organizationName = AJS.$("#organization").val().trim();
    
    var requestUrl = BASE_URL + "/rest/bitbucket/1.0/accountInfo?server=" + encodeURIComponent(repositoryUrl) + "&account=" + encodeURIComponent(organizationName);

    AJS.$.getJSON(requestUrl,
        function(data) {
            AJS.$("#aui-message-bar").empty();

            AJS.$('#Submit').attr("disabled", "");
            if (data.validationErrors && data.validationErrors.length > 0) {
            	AJS.$.each(data.validationErrors, function(i, msg){
            		AJS.messages.error({title : "Error!", body : msg});
            	})
            } else{
            	submitFormAjaxHandler[data.accountType].apply(this, arguments);
        	}
    	}).error(function(a) {
            AJS.$("#aui-message-bar").empty();
            AJS.messages.error({ title: "Error!", 
            	body: "The repository url [<b>" + AJS.escapeHtml(AJS.$("#url").val()) + "</b>] is incorrect or the repository is not responding." 
            });
            AJS.$('#Submit').attr("disabled", "");
        });
    return false;
}

var submitFormAjaxHandler = {

		"bitbucket": function(data){
			
			AJS.$("#repoEntry").attr("action", BASE_URL + "/secure/admin/AddBitbucketOrganization.jspa");

			// hide url input box
			AJS.$('#urlReadOnly').html(AJS.$('#url').val());
			AJS.$('#url').hide(); 
			AJS.$('#urlReadOnly').show();
			
			// hide examples
			AJS.$('#examples').hide();

			//show username / password
			AJS.$("#bitbucket-form-section").fadeIn();
		}, 

		"github":function(data) {
			AJS.$("#repoEntry").attr("action",BASE_URL + "/secure/admin/AddGithubOrganization.jspa");
			if (data.requiresOauth) {
				
				// we need oauth ...
				
				AJS.$('#urlReadOnly').html(AJS.$('#url').val());
				AJS.$('#url').hide(); 
				AJS.$('#urlReadOnly').show();
				
				// hide examples
				AJS.$('#examples').hide();
				
				AJS.$("#github-form-section").fadeIn();

			} else {
				AJS.$('#repoEntry').submit();
			}
		}
}

function deleteOrg() {
	
}

function changePassword() {
	 var popup = new AJS.Dialog({
		 		width: 400, 
		 		height: 300, 
		 		id: "dvcs-change-pass-dialog"
	 });
	 
	 popup.addHeader("Update account credentials");

	 var dialogContent = AJS.$(".update-credentials").clone();
	 popup.addPanel("", dialogContent.html(), "dvcs-update-cred-dialog");
	 
	 popup.addButton("Update", function (dialog) {
         dialog.nextPage();
     });
     popup.addButton("Cancel", function (dialog) {
         dialog.hide();
     });
     
	 popup.show();
}

function syncRepoList() {
	
}

function autoLinkIssuesOrg() {
	
}

//--------------------------------------------------------------------------------------------------
//--------------------------------------------------------------------------------------------------

AJS.$(document).ready(function() {
    if (typeof init_repositories == 'function') {
        init_repositories();
    }

})


