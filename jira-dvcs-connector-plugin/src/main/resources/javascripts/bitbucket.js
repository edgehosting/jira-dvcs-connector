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
		AJS.$('#githube-form-section').hide();
		AJS.$("#repoEntry").attr("action", BASE_URL + "/secure/admin/AddBitbucketOrganization.jspa");

		if (BB_REQUIRES_AUTH == "true") {
			AJS.$("#bitbucket-form-section").fadeIn();
		} else {
			AJS.$("#oauthBbRequired").val("");
		}

	} else if (dvcsType == 1) {

		AJS.$('#bitbucket-form-section').hide();
		AJS.$('#githube-form-section').hide();
		AJS.$("#repoEntry").attr("action", BASE_URL + "/secure/admin/AddGithubOrganization.jspa");
		
		if (GH_REQUIRES_AUTH == "true") {
			AJS.$("#github-form-section").fadeIn();
		} else {
			AJS.$("#oauthRequired").val("");
		}
		
	}  else if (dvcsType == 2) {

		AJS.$('#bitbucket-form-section').hide();
		AJS.$('#github-form-section').hide();

		AJS.$("#repoEntry").attr("action", BASE_URL + "/secure/admin/AddGithubEnterpriseOrganization.jspa");
		
		if (GHE_REQUIRES_AUTH == "true") {
			AJS.$("#githube-form-section").fadeIn();
		} else {
			AJS.$("#oauthRequiredGhe").val("");
		}
	}  

}

//--------------------------------------------------------------------------------------------------
//--------------------------------------------------------------------------------------------------

function forceSync(event, repositoryId) {
	if (event.shiftKey) {
		var dialogTrigger = AJS.$("#jira-dvcs-connector-forceSyncDialog-" + repositoryId);
		var dialog = dialogTrigger.data('jira-dvcs-connector-forceSyncDialog');
		
		if (!dialog) {
			dialog = AJS.InlineDialog(AJS.$("#jira-dvcs-connector-forceSyncDialog-" + repositoryId), "jira-dvcs-connector-forceSyncDialog"  + repositoryId, function(content, trigger, showPopup) {
				content.html(jira.dvcs.connector.plugin.soy.forceSyncDialog({
					'repositoryId' : repositoryId,
				}));
				showPopup();
				return false;
			}, { width: 500, hideDelay: null, noBind: true } );
			dialogTrigger.data('jira-dvcs-connector-forceSyncDialog', dialog);
		}

		dialog.show();
	
	} else {
		softSync(repositoryId);
		
	}
}

function fullSync(repositoryId) {
	AJS.$.post(BASE_URL + "/rest/bitbucket/1.0/repository/" + repositoryId + "/fullsync", function (data) {
		updateSyncStatus(data);
	});
}

function softSync(repositoryId) {
	AJS.$.post(BASE_URL + "/rest/bitbucket/1.0/repository/" + repositoryId + "/softsync", function (data) {
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
            syncStatusHtml = getLastCommitRelativeDateHtml(repo.lastCommitDate);
        } else {
            syncRepoIcon = "running";
            syncStatusHtml = "Synchronizing: <strong>" + repo.sync.changesetCount + "</strong> changesets, <strong>" + repo.sync.jiraCount + "</strong> issues found";
            if (repo.sync.synchroErrorCount > 0)
                syncStatusHtml += ", <span style='color:#e16161;'><strong>" + repo.sync.synchroErrorCount + "</strong> changesets incomplete</span>";
        }
        if (repo.sync.error) {
            syncStatusHtml = "";
            syncIcon = "error";
            syncErrorDiv.html("<span class=\"error\"><strong>Sync Failed:</strong> " + repo.sync.error + "</span>" +
                                "<span style='color:#000;'> &nbsp; &ndash; &nbsp;</span>");
        } else {
        	syncErrorDiv.html("");
    	}
    }
    
    else {
        syncStatusHtml = getLastCommitRelativeDateHtml(repo.lastCommitDate);
    }
    syncIconElement.removeClass("commits").removeClass("finished").removeClass("running").removeClass("error").addClass(syncIcon);
    syncRepoIconElement.removeClass("running").addClass(syncRepoIcon);
    syncStatusDiv.html(syncStatusHtml);

}

function getLastCommitRelativeDateHtml(daysAgo) {
    var html = "";
    if (daysAgo) {
        html = new Date(daysAgo).toDateString();
    }
    return html;
}

function showAddRepoDetails(show) {

	if (!jira.dvcs.connector.plugin.addOrganizationDialog) {
		createAddOrganizationDialog();
	}
	var dialog = jira.dvcs.connector.plugin.addOrganizationDialog;
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
	
	dialog.enabled(true);
	
	// clear all form errors
	DvcsValidator.clearAllErrors();
	
	// enable bitbucket form
	switchDvcsDetailsInternal(0);

	AJS.$("#organization").focus().select();
	dialog.gotoPage(0);
    dialog.gotoPanel(0);
	dialog.show();
	dialog.updateHeight();
}

function createAddOrganizationDialog(action) {
	var dialog = new AJS.Dialog({
		width: 800, 
		height: 400, 
		id: "add-organization-dialog", 
		closeOnOutsideClick: false
	});
	
	// First page
	dialog.addHeader("Add New Account");

	dialog.addPanel("", jira.dvcs.connector.plugin.soy.addOrganizationDialog({
		isGithubEnterpriseEnabled: jira.dvcs.connector.plugin.githubEnterpriseEnabled,
		isOnDemandLicense: jira.dvcs.connector.plugin.onDemandLicense,
		atlToken : jira.dvcs.connector.plugin.atlToken
	}), "panel-body");
	
	dialog.addButtonPanel();

	dialog.page[0].buttonpanel.append("<span id='add-organization-wait' class='aui-icon' style='padding-right:10px'>&nbsp;</span>");
	
	dialog.addSubmit("Add", function (dialog, event) {
		if (dvcsSubmitFormHandler(event,false)) {
			AJS.$("#repoEntry").submit();
		}
	});

	dialog.addCancel("Cancel", function (dialog) {
		AJS.$("#repoEntry").trigger('reset');
		AJS.$("#aui-message-bar").empty();
	    dialog.hide();
	}, "#");
	
	AJS.$('#urlSelect').change(function(event) {
		switchDvcsDetails(event.target);
		dialog.updateHeight();
	});
    
	// Second page, GitHub Enterprise confirmation page
    dialog.addPage();
    dialog.addHeader("Add New Account");
    dialog.addPanel("Confirmation", "<div id='githubeConfirmation'>Test</div>", "panel-body");
    dialog.addSubmit("Continue", function (dialog, event) {
    	dialog.gotoPage(0);
    	if (dvcsSubmitFormHandler(event, true)) {
    		AJS.$("#repoEntry").submit();
    	}
	});

    dialog.addButton("Previous", function(dialog) {
    	   dialog.prevPage();
    	   dialog.updateHeight();
    	});
    
	dialog.addCancel("Cancel", function (dialog) {
		AJS.$("#repoEntry").trigger('reset');
		AJS.$("#aui-message-bar").empty();
	    dialog.hide();
	}, "#");
    
    dialog.enabled = function(enabled) {
    	if (enabled) {
    		AJS.$("#add-organization-wait").removeClass("aui-icon-wait");
    		AJS.$('#add-organization-dialog .button-panel-submit-button').removeAttr("disabled");
    	} else {
    		AJS.$("#add-organization-wait").addClass("aui-icon-wait");
    		AJS.$('#add-organization-dialog .button-panel-submit-button').attr("disabled", "disabled");
    	}
    }
    jira.dvcs.connector.plugin.addOrganizationDialog = dialog;
}

function dvcsSubmitFormHandler(event, skipLoggingAlert) {
    var dialog = jira.dvcs.connector.plugin.addOrganizationDialog;
	// submit form
    var organizationElement = AJS.$("#organization");
    // if not custom URL
    if ( !dvcsContainsSlash( organizationElement.val()) ) {
    	// some really simple validation
    	if (!validateAddOrganizationForm()) {
    		dialog.enabled(true);
    		dialog.updateHeight();
    		return false;
    	}
    	var selectedDvcs = AJS.$("#urlSelect option:selected");
    	var dvcsHost = selectedDvcs.text();
    	
    	if ( selectedDvcs.val() == "githube") { // Github Enterprise
    		// impose real URL to hidden input
    		AJS.$("#url").val(AJS.$("#urlGhe").val()); 

    		if (!skipLoggingAlert) {
    			AJS.$("#githubeConfirmation").html(jira.dvcs.connector.plugin.soy.confirmLoggedIn({
					dvcsHost: dvcsHost
				}));
    			dialog.nextPage();
    			dialog.updateHeight();
    			return false;
    		}
    	}

    	// disable add form
    	dialog.enabled(false);

    	//
        AJS.messages.info({ title: "Connecting to " + dvcsHost + " to configure your account...", closeable : false});
        dialog.updateHeight();
        // set url by selected type
        return true; // submit form
	}

    // else - lets try to identify account
    // account info
    if (!validateAccountInfoForm()) {
    	dialog.enabled(true);
    	return false;
    }
    
    var account = dvcsGetAccountNameFromUrl(AJS.$("#organization").val());
    var dvcsUrl = dvcsGetUrlFromAccountUrl(AJS.$("#organization").val());
    AJS.$("#url").val(dvcsUrl);
    AJS.$("#organization").val(account);

    AJS.$("#aui-message-bar").empty();
    
    AJS.messages.info({ title: "Trying to identify repository type...", closeable : false});
    dialog.updateHeight();
    
    var repositoryUrl = AJS.$("#url").val().trim();
    var organizationName = AJS.$("#organization").val().trim();
    
    var requestUrl = BASE_URL + "/rest/bitbucket/1.0/accountInfo?server=" + encodeURIComponent(repositoryUrl) + "&account=" + encodeURIComponent(organizationName);

    AJS.$.getJSON(requestUrl,
        function(data) {
            
    		AJS.$("#aui-message-bar").empty();
            dialog.enabled(true);
           
            if (data.validationErrors && data.validationErrors.length > 0) {
            	AJS.$.each(data.validationErrors, function(i, msg){
            		AJS.messages.error({title : "Error!", body : msg});
            		dialog.updateHeight();
            	})
            } else{
            	dvcsSubmitFormAjaxHandler[data.dvcsType].apply(this, arguments);
        	}
    	}).error(function(a) {
            AJS.$("#aui-message-bar").empty();
            AJS.messages.error({ title: "Error!", 
            	body: "The url [<b>" + AJS.escapeHtml(AJS.$("#url").val()) + "</b>] is incorrect or the server is not responding." 
            });
            dialog.enabled(true);
            dialog.updateHeight();
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

	if (AJS.$("#oauthClientIdGhe").is(":visible")) {
	    validator.addItem("urlGhe","ghe-url-error", "required");
	    validator.addItem("urlGhe","ghe-invalid-url-error", "url");
		validator.addItem("oauthClientIdGhe", "oauth-ghe-client-error", "required");
		validator.addItem("oauthSecretGhe", "oauth-ghe-secret-error", "required");
	} else if (AJS.$("#oauthBbClientId").is(":visible")) {
		validator.addItem("oauthBbClientId", "oauth-bb-client-error", "required");
		validator.addItem("oauthBbSecret", "oauth-bb-secret-error", "required");
	} else if (AJS.$("#oauthClientId").is(":visible")) {
		validator.addItem("oauthClientId", "oauth-gh-client-error", "required");
		validator.addItem("oauthSecret", "oauth-gh-secret-error", "required");
	} else if (AJS.$("#adminUsername").is(":visible")){
		// validator.addItem("adminUsername", "admin-username-error", "required");
		// validator.addItem("adminPassword", "admin-password-error", "required");
	}
	return validator.runValidation();
}

var dvcsSubmitFormAjaxHandler = {
		"bitbucket": function(data){
			AJS.$("#repoEntry").attr("action", BASE_URL + "/secure/admin/AddBitbucketOrganization.jspa");
			if (data.requiresOauth) {
					
				// hide url input box
				AJS.$('#urlReadOnly').html(AJS.$('#url').val());
				AJS.$('#urlSelect').hide(); 
				AJS.$('#urlReadOnly').show();
				
				//show username / password
				AJS.$("#github-form-section").hide();
				AJS.$("#bitbucket-form-section").fadeIn();
				AJS.$("#adminUsername").focus().select();
				
			} else {

				AJS.$("#oauthBbRequired").val("");
				AJS.$('#repoEntry').submit();

			}
		}, 

		"github": function(data) {
			AJS.$("#repoEntry").attr("action", BASE_URL + "/secure/admin/AddGithubOrganization.jspa");
			if (data.requiresOauth) {

				AJS.$('#urlReadOnly').html(AJS.$('#url').val());
				AJS.$('#urlSelect').hide(); 
				AJS.$('#urlReadOnly').show();
				
				AJS.$("#bitbucket-form-section").hide();
				AJS.$("#github-form-section").fadeIn();
				AJS.$("#oauthClientId").focus().select();

			} else {
				
				AJS.$("#oauthRequired").val("");
				AJS.$('#repoEntry').submit();

			}
		}
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

function configureDefaultGroups(orgName, id) {
	
	// clear all
	AJS.$("#organizationIdDefaultGroups").val("");
	AJS.$("#configureDefaultGroupsContent").html("");
	AJS.$("#configureDefaultGroupsContentWorking").show();
	
	var popup = new AJS.Dialog({
		width: 600, 
		height: 400, 
		id: "dvcs-default-groups-dialog"
	});
	
	AJS.$("#organizationIdDefaultGroups").val(id);

	popup.addHeader("Configure Default Groups");
	var dialogContent = AJS.$(".configure-default-groups");
	popup.addPanel("", "#configureDefaultGroupsForm", "configure-default-groups-dialog");
	popup.addButton("Save", function (dialog) {
		AJS.$("#configureDefaultGroupsForm").submit();
		
	}, "aui-button submit");
	
	popup.addCancel("Cancel", function (dialog) {
		dialog.hide();
	});

	popup.show();
	// load web fragment
	AJS.$.ajax(
			{
				type : 'GET',
				url : BASE_URL + "/rest/bitbucket/1.0/fragment/" + id + "/defaultgroups",
				success :
				function (data) {
					AJS.$("#configureDefaultGroupsContentWorking").hide()
					AJS.$("#configureDefaultGroupsContent").html(data);
				}
			}
		
	).error(function (err) { 
			AJS.$("#configureDefaultGroupsContentWorking").show()
			showError("Unexpected error occurred. Please contact the server administrator.");
		});
}

function configureOAuth(organizationDvcsType, organizationName, organizationId, oAuthKey, oAuthSecret, atlToken) {
	
	function validateField(field, errorMsg) {
		if (!AJS.$.trim(field.val())) {
			field.next().html(errorMsg);
			return false;
		}
		field.next().html("&nbsp;");
		return true;
	}
	
	var popup = new AJS.Dialog({
		width: 600, 
		height: 350, 
		id: "repositoryOAuthDialog"
	});
	
	popup.addHeader("Configure OAuth for account " + organizationName);
	popup.addPanel("", jira.dvcs.connector.plugin.soy.repositoryOAuthDialog({
		'organizationId': organizationId,
		'oAuthKey': oAuthKey,
		'oAuthSecret': oAuthSecret
		}));
	
	popup.addButton("Regenerate Access Token", function (dialog) {
		// validate
		var v1 = validateField(AJS.$("#updateOAuthForm #key"), "OAuth key must not be blank");
		var v2 = validateField(AJS.$("#updateOAuthForm #secret"), "OAuth secret must not be blank");
		if (!v1 || !v2) return;
			
		AJS.$("#repositoryOAuthDialog .dialog-button-panel button").attr("disabled", "disabled");
		AJS.$("#repositoryOAuthDialog .dialog-button-panel").prepend("<span class='aui-icon aui-icon-wait' style='padding-right:10px'>Wait</span>");
		
		// submit form
		AJS.$.post(BASE_URL + "/rest/bitbucket/1.0/org/" + organizationId + "/oauth", AJS.$("#updateOAuthForm").serialize())
			.done(function(data) {

				var actionName;
                if (organizationDvcsType == "bitbucket")
                	actionName="RegenerateBitbucketOauthToken.jspa";
                else if (organizationDvcsType == "github")
                	actionName="RegenerateGithubOauthToken.jspa";
                else
                	actionName="RegenerateGithubEnterpriseOauthToken.jspa";
				
				window.location.replace(BASE_URL+"/secure/admin/"+actionName+"?organization=" + organizationId + "&atl_token="+atlToken);
			})
			.error(function (err) {
				AJS.$("#aui-message-bar").empty();
		        AJS.messages.error({ title: "Error!", 
		          	body: "Could not configure OAuth.",
		          	closeable : false
		        });
		        AJS.$("#repositoryOAuthDialog .dialog-button-panel button").removeAttr("disabled", "disabled");
		        AJS.$("#repositoryOAuthDialog .dialog-button-panel .aui-icon-wait").remove();
		        popup.updateHeight();
			});
	}, "aui-button submit");
	
	popup.addCancel("Cancel", function (dialog) {
		dialog.remove();
	});

	popup.show();
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
			url : BASE_URL + "/rest/bitbucket/1.0/org/" + organizationId + "/autolink",
			data : '{ "payload" : "' + checkedValue+ '"}',
			success :
			function (data) {
				  AJS.$("#" + checkboxId  + "working").hide();
				  AJS.$("#" + checkboxId).removeAttr("disabled");
				  if (!checkedValue && AJS.$("#org_global_smarts" + organizationId)) {
					  AJS.$("#org_global_smarts" + organizationId).attr("disabled", "disabled");
				  } else {
					  AJS.$("#org_global_smarts" + organizationId).removeAttr("disabled");
				  }
			}
		}
	).error(function (err) { 
				  showError("Unexpected error occurred. Please contact the server administrator.");
				  AJS.$("#" + checkboxId  + "working").hide();
				  AJS.$("#" + checkboxId).removeAttr("disabled");
				  setChecked(checkboxId, !checkedValue);
			  });
}

function enableSmartcommitsOnNewRepos(organizationId, checkboxId) {
	var checkedValue = AJS.$("#" + checkboxId).is(':checked');
	AJS.$("#" + checkboxId).attr("disabled", "disabled");
	AJS.$.ajax(
		{
			type : 'POST',
			dataType : "json",
			contentType : "application/json",
			url : BASE_URL + "/rest/bitbucket/1.0/org/" + organizationId + "/globalsmarts",
			data : '{ "payload" : "' + checkedValue+ '"}',
			
			success :
			function (data) {
				AJS.$("#" + checkboxId).removeAttr("disabled");
			}
		}
	  ).error(function (err) {
				  showError("Unexpected error occurred. Please contact the server administrator.");
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
			url : BASE_URL + "/rest/bitbucket/1.0/repo/" + repoId + "/autolink",
			data : '{ "payload" : "' + checkedValue+ '"}',
			
			success :
			function (registration) {
				  if (registration.callBackUrlInstalled != checkedValue) {

					var popup = new AJS.Dialog({
					 		width: 600, 
					 		height: 400, 
					 		id: "dvcs-postcommit-hook-registration-dialog",
					 		closeOnOutsideClick: false
				    });

					popup.addHeader((checkedValue ? "Linking" : "Unlinking") + " the repository");
					popup.addPanel("Registration", jira.dvcs.connector.plugin.soy.postCommitHookDialog({
												'registering': checkedValue,
												'callbackUrl': registration.callBackUrl
												}));
					popup.addButton("Ok", function (dialog) {
						popup.remove();
				    }, "aui-button submit");
					popup.show();
				 }
				  
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
		          err.callbackUrl
				  showError("Unable to " + (checkedValue ? "link" : "unlink") + " selected repository. Please contact the server administrator.");
				  AJS.$("#" + checkboxId  + "working").hide();
				  AJS.$("#" + checkboxId).removeAttr("disabled");
				  setChecked(checkboxId, !checkedValue);
			  });
}

function registerAdminPermissionInlineDialogTooltip() {
	AJS.$(".admin-permission").each(function(index) {
		AJS.InlineDialog(AJS.$(this), "admin-tooltip"+index,
		    function(content, trigger, showPopup) {
				content.css({"padding":"10px"}).html('<p>No admin permission. The post commit hook could not be installed.</p>');
				showPopup();
		        return false;
		    },
		    {onHover:true, hideDelay:200, showDelay:1000, arrowOffsetX:-8, offsetX:-80}
		);
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
			url : BASE_URL + "/rest/bitbucket/1.0/repo/" + repoId + "/smart",
			data : '{ "payload" : "' + checkedValue+ '"}',
			
			success :
			function (data) {
				  AJS.$("#" + checkboxId  + "working").hide();
				  AJS.$("#" + checkboxId).removeAttr("disabled");
			  }
		}
	).error(function (err) { 
				  showError("Unexpected error occurred.");
				  AJS.$("#" + checkboxId  + "working").hide();
				  AJS.$("#" + checkboxId).removeAttr("disabled");
				  setChecked(checkboxId, !checkedValue);
			  });
}

function deleteOrganization(organizationId, organizationName) {
	var answer = confirm("Are you sure you want to remove account '" +organizationName + "' from JIRA ?");
	
	if (answer) {
		var dialog = new AJS.Dialog({width:400, height:150, id:"deleting-account-dialog", closeOnOutsideClick: false});
		dialog.addHeader("Deleting Account");
		dialog.addPanel("DeletePanel", "<span class='dvcs-wait'>Deleting '" + organizationName + "' account. Please wait...</span>");
		dialog.show(); 
		
		AJS.$.ajax({
            url: BASE_URL + "/rest/bitbucket/1.0/organization/" + organizationId,
            type: 'DELETE',
            success: function(result) {
                window.location.reload();
            }
        }).error(function (err) { 
        	dialog.remove();
        	showError("Error when deleting account '" + organizationName + "'.");
		});
	}
}

function syncRepositoryList(organizationId,organizationName) {
	var dialog = new AJS.Dialog({width:400, height:150, id:"refreshing-account-dialog", closeOnOutsideClick: false});
	dialog.addHeader("Refreshing Account");
	dialog.addPanel("RefreshPanel", "<span class='dvcs-wait'>Refreshing '" + organizationName + "' account. Please wait...</span>");
	dialog.show(); 
	
	AJS.$.ajax({
        url: BASE_URL + "/rest/bitbucket/1.0/organization/" + organizationId + "/syncRepoList",
        type: 'GET',
        success: function(result) {
            window.location.reload();
        }
    }).error(function (err) { 
    	window.location.reload();
	});
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
		"github" : "https://github.com",
		"githube" : "https://github.com"
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
    	//
    	if (window.location.hash == '#expand') {
    		showAddRepoDetails(true);
    	}
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
