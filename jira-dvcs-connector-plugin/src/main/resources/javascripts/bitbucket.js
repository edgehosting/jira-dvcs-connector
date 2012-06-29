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


function switchDvcsDetails(selectSwitch) {
	
	
	var dvcsType = selectSwitch.selectedIndex;
	switchDvcsDetailsInternal(dvcsType);
}

function switchDvcsDetailsInternal(dvcsType) {
	
	// clear all form errors
	DvcsValidator.clearAllErrors();

	// impose real URL to hidden input
	AJS.$("#url").val( dvcsKnownUrls[AJS.$("#urlSelect option:selected").val()] ); 
	AJS.$("#organization").focus().select();

	if (dvcsType == 0) {
		
		
		AJS.$('#github-form-section').hide();
		
		AJS.$("#repoEntry").attr("action", BASE_URL + "/secure/admin/AddBitbucketOrganization.jspa");

		// hide examples
		AJS.$('#examples').hide();

		//show username / password
		AJS.$("#bitbucket-form-section").fadeIn();
		

	} else if (dvcsType == 1) {

		AJS.$('#bitbucket-form-section').hide();
		AJS.$("#repoEntry").attr("action", BASE_URL + "/secure/admin/AddGithubOrganization.jspa");
		
		if (GH_REQUIRES_AUTH == "true") {
			
			// we need oauth ...
			
			// hide examples
			AJS.$('#examples').hide();
			
			AJS.$("#github-form-section").fadeIn();

		} else {
			
			AJS.$("#oauthRequired").val("");

		}
		
		
	}  
}

//--------------------------------------------------------------------------------------------------
//--------------------------------------------------------------------------------------------------
function forceSync(repositoryId) {
	AJS.$.post(BASE_URL + "/rest/bitbucket/1.0/repository/" + repositoryId + "/sync", function (data) {
		updateSyncStatus(data);
	});
}

function retrieveSyncStatus() {

	AJS.$.getJSON(BASE_URL + "/rest/bitbucket/1.0/repositories", function (data) {

		AJS.$.each(data.repositories, function (a, repo) {
			updateSyncStatus(repo);
		});
	
		window.setTimeout(retrieveSyncStatus, 4000)
	
	});
}

function updateSyncStatus(repo) {

	var syncStatusDiv = AJS.$('#sync_status_message_' + repo.id);
    var syncErrorDiv = AJS.$('#sync_error_message_' + repo.id);
    var syncIconElement = AJS.$('#syncicon_' + repo.id);
    var syncRepoIconElement = AJS.$('#syncrepoicon_' + repo.id);

    var syncStatusHtml = "";
    var syncIcon;
    var syncRepoIcon;

    if (repo.sync) {

        if (repo.sync.finished) {
            if (repo.lastCommitDate) {
//            	syncIcon = "commits";
            }
            syncStatusHtml = getLastCommitRelativeDateHtml(repo.lastCommitDate);

        } else {
//            syncIcon = "running";
            syncRepoIcon = "running";
            syncStatusHtml = "Synchronizing: <strong>" + repo.sync.changesetCount + "</strong> changesets, <strong>" + repo.sync.jiraCount + "</strong> issues found";
            if (repo.sync.synchroErrorCount > 0)
                syncStatusHtml += ", <span style='color:#e16161;'><strong>" + repo.sync.synchroErrorCount + "</strong> changesets incomplete</span>";

        }
        if (repo.sync.error) {
            syncStatusHtml = "";
            syncIcon = "error";
//            syncErrorDiv.html("<div class=\"error\"><strong>Sync Failed:</strong> " + repo.sync.error + "</div>");
            syncErrorDiv.html("<span class=\"error\"><strong>Sync Failed:</strong> " + repo.sync.error + "</span>" +
                                "<span style='color:#000;'> &nbsp; &ndash; &nbsp;</span>");
        } else {
        	syncErrorDiv.html("");
    	}
    }
    
    else {
        if (repo.lastCommitDate) {
//        	syncIcon = "commits";
        }
        syncStatusHtml = getLastCommitRelativeDateHtml(repo.lastCommitDate);
    }
    syncIconElement.removeClass("commits").removeClass("finished").removeClass("running").removeClass("error").addClass(syncIcon);
    syncRepoIconElement.removeClass("running").addClass(syncRepoIcon);

//    if (syncStatusHtml != "") {
//    	syncStatusHtml += " <span style='color:#000;'> &nbsp; &ndash; &nbsp;</span>";
//    }
    syncStatusDiv.html(syncStatusHtml);

}

function getLastCommitRelativeDateHtml(daysAgo) {
	    var html = "";
	    if (daysAgo) {
//	        html = "last commit " + new Date(daysAgo).toDateString();
	        html = new Date(daysAgo).toDateString();
	    }
	    return html;
}

function showAddRepoDetails(show) {

	if (show) {

		// Reset to default view:

		AJS.$('#repoEntry').attr("action", "");
		
		// - hide username/password
		AJS.$("#github-form-section").hide();

		// - show url, organization field
		AJS.$('#urlSelect').show();
		AJS.$('#urlSelect').val(0); // select BB by default
		AJS.$('#urlReadOnly').hide();

		AJS.$('#organization').show();
		AJS.$('#organizationReadOnly').hide();

		//
		AJS.$('#Submit').removeAttr("disabled");

		// show examples
		AJS.$('#examples').show();
		
		// clear all form errors
		DvcsValidator.clearAllErrors();
		
		// enable bitbucket form
		switchDvcsDetailsInternal(0);

		AJS.$('#linkRepositoryButton').fadeOut(function() {
			AJS.$('#addRepositoryDetails').slideDown();
			AJS.$("#organization").focus().select();
		});

	} else {

		AJS.$('#addRepositoryDetails').slideUp(function() {
			AJS.$('#linkRepositoryButton').fadeIn();
			AJS.$("#organization").focus().select();
		});

	}
}
function dvcsSubmitFormHandler() {

    AJS.$('#Submit').attr("disabled", "disabled");

    // submit form
    var organizationElement = AJS.$("#organization");
    
    // if not custom URL
    if ( !dvcsContainsSlash( organizationElement.val()) ) {
    	// some really simple validation
    	if (!validateAddOrganizationForm()) {
    		AJS.$('#Submit').removeAttr("disabled");
    		return false;
    	}
    	var dvcsHost = AJS.$("#urlSelect option:selected").text();
    	//
        AJS.messages.info({ title: "Connecting to " + dvcsHost + " to configure your account...", closeable : false});
        // set url by selected type
        return true; // submit form
	}

    // else - lets try to identify account
    
    
    // account info
    
    if (!validateAccountInfoForm()) {
    	AJS.$('#Submit').removeAttr("disabled");
    	return false;
    }
    
    var account = dvcsGetAccountNameFromUrl(AJS.$("#organization").val());
    var dvcsUrl = dvcsGetUrlFromAccountUrl(AJS.$("#organization").val());
    AJS.$("#url").val(dvcsUrl);
    AJS.$("#organization").val(account);

    AJS.$("#aui-message-bar").empty();
    
    AJS.messages.info({ title: "Trying to identify repository type...", closeable : false});

    var repositoryUrl = AJS.$("#url").val().trim();
    var organizationName = AJS.$("#organization").val().trim();
    
    var requestUrl = BASE_URL + "/rest/bitbucket/1.0/accountInfo?server=" + encodeURIComponent(repositoryUrl) + "&account=" + encodeURIComponent(organizationName);

    AJS.$.getJSON(requestUrl,
        function(data) {
            
    		AJS.$("#aui-message-bar").empty();
            AJS.$('#Submit').removeAttr("disabled");
           
            if (data.validationErrors && data.validationErrors.length > 0) {
            	AJS.$.each(data.validationErrors, function(i, msg){
            		AJS.messages.error({title : "Error!", body : msg});
            	})
            } else{
            	dvcsSubmitFormAjaxHandler[data.dvcsType].apply(this, arguments);
        	}
    	}).error(function(a) {
            AJS.$("#aui-message-bar").empty();
            AJS.messages.error({ title: "Error!", 
            	body: "The url [<b>" + AJS.escapeHtml(AJS.$("#url").val()) + "</b>] is incorrect or the server is not responding." 
            });
            AJS.$('#Submit').removeAttr("disabled");
        });
    return false;
}

function validateAccountInfoForm() {

	var validator = new DvcsValidator();
	
	validator.addItem("organization", "org-error", "required");

	return validator.runValidation();

}

function validateAddOrganizationForm() {
	
	var validator = new DvcsValidator();
	
	validator.addItem("organization", "org-error", "required");
	
	if (AJS.$("#oauthClientId").is(":visible")) {
		validator.addItem("oauthClientId", "oauth-client-error", "required");
		validator.addItem("oauthSecret", "oauth-secret-error", "required");
	} else if (AJS.$("#adminUsername").is(":visible")){
		// validator.addItem("adminUsername", "admin-username-error", "required");
		// validator.addItem("adminPassword", "admin-password-error", "required");
	}
	
	return validator.runValidation();
	
}

// TBD
var dvcsSubmitFormAjaxHandler = {

		"bitbucket": function(data){
			
			AJS.$("#repoEntry").attr("action", BASE_URL + "/secure/admin/AddBitbucketOrganization.jspa");

			// hide url input box
			AJS.$('#urlReadOnly').html(AJS.$('#url').val());
			AJS.$('#urlSelect').hide(); 
			AJS.$('#urlReadOnly').show();
			
			// hide examples
			AJS.$('#examples').hide();

			//show username / password
			AJS.$("#github-form-section").hide();
			AJS.$("#bitbucket-form-section").fadeIn();
			AJS.$("#adminUsername").focus().select();
		}, 

		"github": function(data) {
			
			AJS.$("#repoEntry").attr("action", BASE_URL + "/secure/admin/AddGithubOrganization.jspa");
			
			if (data.requiresOauth) {
				
				// we need oauth ...
				
				AJS.$('#urlReadOnly').html(AJS.$('#url').val());
				AJS.$('#urlSelect').hide(); 
				AJS.$('#urlReadOnly').show();
				
				// hide examples
				AJS.$('#examples').hide();
				
				AJS.$("#bitbucket-form-section").hide();
				AJS.$("#github-form-section").fadeIn();
				AJS.$("#oauthClientId").focus().select();

			} else {
				
				AJS.$("#oauthRequired").val("");
				AJS.$('#repoEntry').submit();

			}
		}
}

function deleteOrg() {
	
}

function changePassword(username, id) {
	
	 // clear all
	 AJS.$("#organizationId").val("");
	 AJS.$("#usernameUp").val("");
	 AJS.$("#adminPasswordUp").val("");
	
	 var popup = new AJS.Dialog({
		 		width: 400, 
		 		height: 300, 
		 		id: "dvcs-change-pass-dialog"
	 });
	 
	 AJS.$("#organizationId").val(id);
	 AJS.$("#usernameUp").val(username);

	 popup.addHeader("Update Account Credentials");

	 var dialogContent = AJS.$(".update-credentials");

	 popup.addPanel("", "#updatePasswordForm", "dvcs-update-cred-dialog");
	 
	 popup.addButton("Update", function (dialog) {
        
		 AJS.$("#updatePasswordForm").submit();
        
     }, "aui-button submit");
     popup.addButton("Cancel", function (dialog) {
         dialog.hide();
     }, "aui-button submit");
     
	 popup.show();
	 
	 AJS.$("#adminPasswordUp").focus().select();
}


function autoLinkIssuesOrg(organizationId, checkboxId) {
	
	var checkedValue = AJS.$("#" + checkboxId).is(':checked');
	AJS.$("#" + checkboxId).attr("disabled", "disabled");

	AJS.$("#" + checkboxId  + "working").show();
	
	AJS.$.ajax(
		{
			type : 'POST',
			dataType : "json",
			contentType : "application/json",
			  
			url :
			BASE_URL + "/rest/bitbucket/1.0/org/" + organizationId + "/autolink",
			
			data :
			'{ "payload" : "' + checkedValue+ '"}',
			  
			success :
			function (data) {
				  AJS.$("#" + checkboxId  + "working").hide();
				  AJS.$("#" + checkboxId).removeAttr("disabled");
			}
		}
	).error(function (err) { 
				  showError("Unexpected error occured. Please contact the server admnistrator.");
				  AJS.$("#" + checkboxId  + "working").hide();
				  AJS.$("#" + checkboxId).removeAttr("disabled");
				  setChecked(checkboxId, !checkedValue);
			  });
}

function autoInviteNewUser(organizationId, checkboxId) {
	
	var checkedValue = AJS.$("#" + checkboxId).is(':checked');
	
	AJS.$("#" + checkboxId).attr("disabled", "disabled");
	
	AJS.$("#" + checkboxId  + "working").show();
	
	AJS.$.ajax(
		{
			type : 'POST',
			dataType : "json",
			contentType : "application/json",
			  
			url :
			BASE_URL + "/rest/bitbucket/1.0/org/" + organizationId + "/autoinvite",
			
			data :
			'{ "payload" : "' + checkedValue+ '"}',
			
			success :
			function (data) {
				AJS.$("#" + checkboxId  + "working").hide();
				AJS.$("#" + checkboxId).removeAttr("disabled");
			}
		}
	  ).error(function (err) { 
				  showError("Unexpected error occured. Please contact the server admnistrator.");
				  AJS.$("#" + checkboxId  + "working").hide();
				  AJS.$("#" + checkboxId).removeAttr("disabled");
				  setChecked(checkboxId, !checkedValue);
			  });
}

function enableOrgGlobalSmartcommits(organizationId, checkboxId) {
	
	var checkedValue = AJS.$("#" + checkboxId).is(':checked');
	
	AJS.$("#" + checkboxId).attr("disabled", "disabled");
	
	AJS.$.ajax(
		{
			type : 'POST',
			dataType : "json",
			contentType : "application/json",
			  
			url :
			BASE_URL + "/rest/bitbucket/1.0/org/" + organizationId + "/globalsmarts",
			
			data :
			'{ "payload" : "' + checkedValue+ '"}',
			
			success :
			function (data) {
				AJS.$("#" + checkboxId).removeAttr("disabled");
				if (checkedValue) {
					AJS.$(".repo_smartcommit_check" + organizationId).attr("disabled", "disabled");
				} else {
					AJS.$(".repo_smartcommit_check" + organizationId).removeAttr("disabled");
				}
			}
		}
	  ).error(function (err) {
				  showError("Unexpected error occured. Please contact the server admnistrator.");
				  setChecked(checkboxId, !checkedValue);
	  });
}

function autoLinkIssuesRepo(repoId, checkboxId) {
	
	var checkedValue = AJS.$("#" + checkboxId).is(":checked");
	AJS.$("#" + checkboxId).attr("disabled", "disabled");

	AJS.$("#" + checkboxId  + "working").show();

	AJS.$.ajax( 
		{
			type : 'POST',
			dataType : "json",
			contentType : "application/json",
			  
			url :
			BASE_URL + "/rest/bitbucket/1.0/repo/" + repoId + "/autolink",
			
			data :
			'{ "payload" : "' + checkedValue+ '"}',
			
			success :
			function (data) {
				  AJS.$("#" + checkboxId  + "working").hide();
				  AJS.$("#" + checkboxId).removeAttr("disabled");
				  if (checkedValue) {
					  AJS.$("#dvcs-action-container-" + repoId).removeClass("dvcs-nodisplay");
					  AJS.$("#dvcs-action-container2-" + repoId).removeClass("dvcs-nodisplay");
					  AJS.$("#dvcs-action-container3-" + repoId).removeClass("dvcs-nodisplay");
					  AJS.$("#dvcs-repo-row-" + repoId).removeClass("dvcs-disabled");
				  } else {
					  AJS.$("#dvcs-action-container-" + repoId).addClass("dvcs-nodisplay");
					  AJS.$("#dvcs-action-container2-" + repoId).addClass("dvcs-nodisplay");
					  AJS.$("#dvcs-action-container3-" + repoId).addClass("dvcs-nodisplay");
					  AJS.$("#dvcs-repo-row-" + repoId).addClass("dvcs-disabled");
				  }
			  }
		}
	 
	).error(function (err) { 
				  showError("Unable to link selected repository. Do you have administrator permissions?");
				  AJS.$("#" + checkboxId  + "working").hide();
				  AJS.$("#" + checkboxId).removeAttr("disabled");
				  setChecked(checkboxId, !checkedValue);
			  });
}

function enableRepoSmartcommits(repoId, checkboxId) {
	
	var checkedValue = AJS.$("#" + checkboxId).is(":checked");
	AJS.$("#" + checkboxId).attr("disabled", "disabled");

	AJS.$("#" + checkboxId  + "working").show();

	AJS.$.ajax( 
		{
			type : 'POST',
			dataType : "json",
			contentType : "application/json",
			  
			url :
			BASE_URL + "/rest/bitbucket/1.0/repo/" + repoId + "/smart",
			
			data :
			'{ "payload" : "' + checkedValue+ '"}',
			
			success :
			function (data) {
				  AJS.$("#" + checkboxId  + "working").hide();
				  AJS.$("#" + checkboxId).removeAttr("disabled");
			  }
		}
	 
	).error(function (err) { 
				  showError("Unexpected error occured.");
				  AJS.$("#" + checkboxId  + "working").hide();
				  AJS.$("#" + checkboxId).removeAttr("disabled");
				  setChecked(checkboxId, !checkedValue);
			  });
}

function confirmDeleteOrganization(organization) {
	return confirm("Are you sure you want to remove account '" + organization + "' from JIRA ?");
}

function showError(message) {
	alert(message);
}

function setChecked(checkboxId, checked) {
	if (checked) {
		AJS.$("#" + checkboxId).attr("checked", "checked");
	} else {
		AJS.$("#" + checkboxId).removeAttr("checked");
	}
}

function dvcsShowHidePanel(id) {
	var jqElement = AJS.$("#" + id);
	if (jqElement.is(":visible")) {
		AJS.$("#" + id).fadeOut().slideUp();
	} else {
		AJS.$("#" + id).fadeIn().slideDown();
	}
}
//--------------------------------------------------------------------------------------------------
//--------------------------------------------------------------------------------------------------

var dvcsKnownUrls = {
		
		"bitbucket" : "https://bitbucket.org",
		"github" : "https://github.com"
};

function dvcsContainsSlash(stringType) {
	
	return stringType.indexOf("/") != -1;
	
};

function dvcsGetAccountNameFromUrl (stringType) {
	
	// case of i.e. https://bitbucket.org/someuser/
	if (dvcsEndsWith(stringType, "/")) {
		stringType = stringType.substring(0, stringType.length - 1);
	}

	var extracted = stringType.substring(stringType.lastIndexOf("/") + 1, stringType.length);
	return extracted.replace("/");
}

function dvcsGetUrlFromAccountUrl (stringType) {
	
	// case of i.e. https://bitbucket.org/someuser/
	if (dvcsEndsWith(stringType, "/")) {
		stringType = stringType.substring(0, stringType.length - 1);
	}

	return stringType.substring(0, stringType.lastIndexOf("/"));
}

function dvcsEndsWith (stringType, suffix) {
	  return stringType.indexOf(suffix, stringType.length - suffix.length) !== -1;
}

function dvcsCopyOrganizationToUsername() {
	if (AJS.$("#organization").val() && !dvcsContainsSlash(AJS.$("#organization").val())) {
		if (AJS.$("#adminUsername").val().length == 0) {
			AJS.$("#adminUsername").val(AJS.$("#organization").val());
			AJS.$("#adminUsername").focus().select();
		}
	}
}
//------------------------------------------------------------

AJS.$(document).ready(function() {
	
    if (typeof init_repositories == 'function') {
    	
    	// cancel annoying leave message even when browser pre-fill some fields
    	window.onbeforeunload = function () {};
    	
    	// some organization gear
    	AJS.$(".dvcs-organization-controls-tool").dvcsGearMenu(
    			{ noHideItemsSelector : ".dvcs-gearmenu-nohide" }
    	);

    	// defined in macro
    	init_repositories();
    }

});

//---------------------------------------------------------

AJS.$.fn.extend({
	    dvcsGearMenu : function(opts) {
	    	// original AUI dropdown
	    	this.dropDown();
	    	// stop further propagation - causes not hide dropdown menu
	    	AJS.$(opts.noHideItemsSelector).bind("click", function (e){
	    		e.stopPropagation();
	    	});
	    }
});

