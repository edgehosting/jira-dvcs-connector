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
        AJS.$("#bitbucket-form-section").fadeIn();

    } else if (dvcsType == 1) {

        AJS.$('#bitbucket-form-section').hide();
        AJS.$('#githube-form-section').hide();
        AJS.$("#repoEntry").attr("action", BASE_URL + "/secure/admin/AddGithubOrganization.jspa");
        AJS.$("#github-form-section").fadeIn();
        
    }  else if (dvcsType == 2) {

        AJS.$('#bitbucket-form-section').hide();
        AJS.$('#github-form-section').hide();
        AJS.$("#repoEntry").attr("action", BASE_URL + "/secure/admin/AddGithubEnterpriseOrganization.jspa");
        AJS.$("#githube-form-section").fadeIn();
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
        atlToken : jira.dvcs.connector.plugin.atlToken,
        oAuthStore : jira.dvcs.connector.plugin.oAuthStore
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
        dialog.updateHeight();
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
            AJS.$('#add-organization-dialog .button-panel-submit-button').remove("aria-disabled");
        } else {
            AJS.$("#add-organization-wait").addClass("aui-icon-wait");
            AJS.$('#add-organization-dialog .button-panel-submit-button').attr("disabled", "disabled");
            AJS.$('#add-organization-dialog .button-panel-submit-button').attr("aria-disabled", "true");
        }
    }
    jira.dvcs.connector.plugin.addOrganizationDialog = dialog;
}

function dvcsSubmitFormHandler(event, skipLoggingAlert) {
    var dialog = jira.dvcs.connector.plugin.addOrganizationDialog;
    // submit form
    var organizationElement = AJS.$("#organization");
    // if not custom URL
    if ( !parseAccountUrl( organizationElement.val()) ) {
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
        AJS.messages.info("#aui-message-bar", { title: "Connecting to " + dvcsHost + " to configure your account...", closeable : false});
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
    
    var account = parseAccountUrl(AJS.$("#organization").val());
    AJS.$("#url").val(account.hostUrl);
    AJS.$("#organization").val(account.name);

    AJS.$("#aui-message-bar").empty();
    
    AJS.messages.info("#aui-message-bar", { title: "Trying to identify repository type...", closeable : false});
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
                    AJS.messages.error("#aui-message-bar", {title : "Error!", body : msg});
                    dialog.updateHeight();
                })
            } else{
                dvcsSubmitFormAjaxHandler[data.dvcsType].apply(this, arguments);
            }
        }).error(function(a) {
            AJS.$("#aui-message-bar").empty();
            AJS.messages.error("#aui-message-bar", { title: "Error!", 
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
        AJS.$('#repoEntry').submit();
    }, 

    "github": function(data) {
        AJS.$("#repoEntry").attr("action", BASE_URL + "/secure/admin/AddGithubOrganization.jspa");
        AJS.$('#repoEntry').submit();
    }
}

function configureDefaultGroups(orgName, id) {
    
    // clear all
    AJS.$("#organizationIdDefaultGroups").val("");
    AJS.$("#configureDefaultGroupsContent").html("");
    AJS.$("#configureDefaultGroupsContentWorking").show();
    AJS.$("#aui-message-bar-default-groups").empty();
    
    AJS.$("#organizationIdDefaultGroups").val(id);
    
    // we need to copy dialog content as dialog will destroy it when removed
    var dialogContent = AJS.$("#configureDefaultGroupsContainer").html();
    
    var dialog = confirmationDialog({
        header: "Configure automatic access",
        body: dialogContent,
        submitButtonLabel: "Save",
        okAction: function (dialog) { AJS.$("#configureDefaultGroupsForm").submit(); }
        });

    dialog.page[0].buttonpanel.append("<span class='dialog-help'>Help on <a href='https://confluence.atlassian.com/x/Bw4zDQ' target='_blank'>account management</a></span>");
    dialog.disableSubmitButton();
    
    // load web fragment
    AJS.$.ajax({
            type : 'GET',
            url : BASE_URL + "/rest/bitbucket/1.0/fragment/" + id + "/defaultgroups",
            success :
            function (data) {
            	if (dialog.isAttached())
            	{
	            	// we need to reference .dialog-panel-body as this is copy
	                AJS.$(".dialog-panel-body #configureDefaultGroupsContentWorking").hide()
	                AJS.$(".dialog-panel-body #configureDefaultGroupsContent").html(data);
	                dialog.updateHeight();
	                dialog.enableSubmitButton();
            	}
            }
        }
    ).error(function (err) { 
            dialog.showError("Unexpected error occurred. Please contact the server administrator.");
        });
}

function configureOAuth(org, atlToken) {
    function validateField(field, errorMsg) {
        if (!AJS.$.trim(field.val())) {
            showError(field, errorMsg);
            return false;
        }
        clearError(field);
        return true;
    }
    
    function showError(field, errorMsg) {
    	field.next().html(errorMsg);
        field.next().show();
    }
    
    function clearError(field) {
    	field.next().html("&nbsp;");
    	field.next().hide();
    }
    
    var popup = new AJS.Dialog({
        width: 600, 
        height: 350, 
        id: "repositoryOAuthDialog"
    });
    
    popup.addHeader("Configure OAuth for account " + org.name);
    popup.addPanel("", jira.dvcs.connector.plugin.soy.repositoryOAuthDialog({
        'organizationId': org.id,
        'oAuthKey': org.credential.key,
        'oAuthSecret': org.credential.secret,
        'isOnDemandLicense': jira.dvcs.connector.plugin.onDemandLicense
        }));
    
    clearError(AJS.$("#updateOAuthForm #key"));
    clearError(AJS.$("#updateOAuthForm #secret"));
    popup.addButton("Regenerate Access Token", function (dialog) {
        // validate
        var v1 = validateField(AJS.$("#updateOAuthForm #key"), "OAuth key must not be blank");
        var v2 = validateField(AJS.$("#updateOAuthForm #secret"), "OAuth secret must not be blank");
        popup.updateHeight();
        if (!v1 || !v2) return;
            
        AJS.$("#repositoryOAuthDialog .dialog-button-panel button").attr("disabled", "disabled");
        AJS.$("#repositoryOAuthDialog .dialog-button-panel button").attr("aria-disabled", "true");
        AJS.$("#repositoryOAuthDialog .dialog-button-panel").prepend("<span class='aui-icon aui-icon-wait' style='padding-right:10px'>Wait</span>");
        
        // submit form
        AJS.$.post(BASE_URL + "/rest/bitbucket/1.0/org/" + org.id + "/oauth", AJS.$("#updateOAuthForm").serialize())
            .done(function(data) {

                var actionName;
                if (org.dvcsType == "bitbucket")
                    actionName="RegenerateBitbucketOauthToken.jspa";
                else if (org.dvcsType == "github")
                    actionName="RegenerateGithubOauthToken.jspa";
                else
                    actionName="RegenerateGithubEnterpriseOauthToken.jspa";
                
                window.location.replace(BASE_URL+"/secure/admin/"+actionName+"?organization=" + org.id + "&atl_token="+atlToken);
            })
            .error(function (err) {
                AJS.$("#aui-message-bar-oauth-dialog").empty();
                AJS.messages.error("#aui-message-bar-oauth-dialog", { title: "Error!", 
                      body: "Could not configure OAuth.",
                      closeable : false
                });
                AJS.$("#repositoryOAuthDialog .dialog-button-panel button").removeAttr("disabled", "disabled");
                AJS.$("#repositoryOAuthDialog .dialog-button-panel button").removeAttr("aria-disabled");
                AJS.$("#repositoryOAuthDialog .dialog-button-panel .aui-icon-wait").remove();
                popup.updateHeight();
            });
    }, "aui-button submit");
    
    popup.addCancel("Cancel", function (dialog) {
        dialog.remove();
    });
    
    popup.show();
    popup.updateHeight();

    AJS.$.getJSON(BASE_URL + "/rest/bitbucket/1.0/organization/" + org.id + "/tokenOwner", function(data) {
        AJS.$(".repositoryOAuthDialog #tokenUser").html(jira.dvcs.connector.plugin.soy.repositoryOAuthDialogTokenOwner(data));
    }).error(function (err) { 
    	AJS.$(".repositoryOAuthDialog #tokenUser").html("<i>&lt;Invalid, please regenerate access token.&gt;<i>");
    });
    
    return false;
}

function registerDropdownCheckboxHandlers() {
	AJS.$("a[id^='org_autolink_check']").on({
		"aui-dropdown2-item-check": function() {
			autoLinkIssuesOrg(this.id.substring("org_autolink_check".length), this.id, true);
		},
		"aui-dropdown2-item-uncheck": function() {
			autoLinkIssuesOrg(this.id.substring("org_autolink_check".length), this.id, false);
		}
	});
	
	AJS.$("a[id^='org_global_smarts']").on({
		"aui-dropdown2-item-check": function() {
			enableSmartcommitsOnNewRepos(this.id.substring("org_global_smarts".length), this.id, true);
		},
		"aui-dropdown2-item-uncheck": function() {
			enableSmartcommitsOnNewRepos(this.id.substring("org_global_smarts".length), this.id, false);
		}
	});
}

function autoLinkIssuesOrg(organizationId, checkboxId, checkedValue) {
    AJS.$("#" + checkboxId).addClass("disabled");
    AJS.$.ajax({
            type : 'POST',
            dataType : "json",
            contentType : "application/json",
            url : BASE_URL + "/rest/bitbucket/1.0/org/" + organizationId + "/autolink",
            data : '{ "payload" : "' + checkedValue+ '"}',
            success :
            function (data) {
                  AJS.$("#" + checkboxId).removeClass("disabled");
                  if (!checkedValue && AJS.$("#org_global_smarts" + organizationId)) {
                      AJS.$("#org_global_smarts" + organizationId).addClass("disabled");
                  } else {
                      AJS.$("#org_global_smarts" + organizationId).removeClass("disabled");
                  }
            }
        }
    ).error(function (err) { 
          showError("Unexpected error occurred. Please contact the server administrator.", "#aui-message-bar-"+organizationId);
          AJS.$("#" + checkboxId).removeClass("disabled");
          setCheckedDropdown2(checkboxId, !checkedValue);
      });
}

function enableSmartcommitsOnNewRepos(organizationId, checkboxId, checkedValue) {
    AJS.$("#" + checkboxId).addClass("disabled");
    AJS.$.ajax({
            type : 'POST',
            dataType : "json",
            contentType : "application/json",
            url : BASE_URL + "/rest/bitbucket/1.0/org/" + organizationId + "/globalsmarts",
            data : '{ "payload" : "' + checkedValue+ '"}',
            success :
            function (data) {
                AJS.$("#" + checkboxId).removeClass("disabled");
            }
        }
      ).error(function (err) {
              showError("Unexpected error occurred when enabling smart commits on new repositories. Please contact the server administrator.", "#aui-message-bar-"+organizationId);
              AJS.$("#" + checkboxId).removeClass("disabled");
              setCheckedDropdown2(checkboxId, !checkedValue);
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
                    popup.updateHeight();
                    
                    // show warning icon if not already shown
                    var errorStatusIcon = AJS.$("#error_status_icon_" +repoId);
                    errorStatusIcon.addClass("admin_permission aui-icon aui-icon-warning");
                    registerAdminPermissionInlineDialogTooltip(errorStatusIcon);
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
              var errorStatusIcon = AJS.$("#error_status_icon_" +repoId);
              errorStatusIcon.removeClass("admin_permission aui-icon-warning").addClass("aui-icon aui-icon-error");
              var response = AJS.$.parseJSON(err.responseText);
              var message = "";
              if (response) {
            	  message = "<p>" + response.message + "</p>";
              }
              var tooltip = registerInlineDialogTooltip(errorStatusIcon, "Unable to " + (checkedValue ? "link" : "unlink") + " selected repository", message + "<p>Please contact the server administrator.</p>");
              tooltip.show();
              AJS.$("#" + checkboxId  + "working").hide();
              AJS.$("#" + checkboxId).removeAttr("disabled");
              setChecked(checkboxId, !checkedValue);
          });
}

function registerAdminPermissionInlineDialogTooltips() {
    AJS.$(".admin-permission").each(function(index) {
        registerAdminPermissionInlineDialogTooltip(this);
    });
}

function registerAdminPermissionInlineDialogTooltip(element) {
    registerInlineDialogTooltip(element, "No admin permission", "The post commit hook could not be installed.");
}

function registerInlineDialogTooltip(element, title, body) {
    return AJS.InlineDialog(AJS.$(element), "tooltip_"+AJS.$(element).attr('id'),
            function(content, trigger, showPopup) {
                content.css({"padding":"10px"}).html("<h2>"+ title + "</h2><div>" + body + "</div>");
                showPopup();
                return false;
            },
            {onHover:true, hideDelay:200, showDelay:1000, arrowOffsetX:-8, offsetX:-80}
        );
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

function confirmationDialog(options) {
    var dialog = new AJS.Dialog({width:500, height:150, id: "confirm-dialog", closeOnOutsideClick: false});
    dialog.addHeader(options.header);
    dialog.addPanel("ConfirmPanel", options.body);
    
    dialog.addButtonPanel();
    dialog.page[0].buttonpanel.append("<span id='confirm-action-wait' class='aui-icon' style='padding-right:10px'>&nbsp;</span>");
    
    dialog.addSubmit(options.submitButtonLabel, function (dialog, event) {
        dialog.working(true);
        if (typeof options.okAction == 'function') {
            options.okAction(dialog);
        }
    });
    
    dialog.addCancel("Cancel", function (dialog) {
        if (typeof options.cancelAction == 'function') {
            options.cancelAction(dialog);
        }
        dialog.remove();
    }, "#");

    dialog.disableSubmitButton = function() {
    	AJS.$('#confirm-dialog .button-panel-submit-button').attr("disabled", "disabled");
    	AJS.$('#confirm-dialog .button-panel-submit-button').attr("aria-disabled", "true");
    }
    
    dialog.enableSubmitButton = function() {
    	AJS.$('#confirm-dialog .button-panel-submit-button').removeAttr("disabled");
    	AJS.$('#confirm-dialog .button-panel-submit-button').removeAttr("aria-disabled");
    }
    
    dialog.working = function(working) {
        if (working) {
            AJS.$("#confirm-action-wait").addClass("aui-icon-wait");
            this.disableSubmitButton();
        } else {
            AJS.$("#confirm-action-wait").removeClass("aui-icon-wait");
            this.enableSubmitButton();
        }
    }
    
    dialog.showError = function(message) {
        dialog.working(false);
        showError(message, "#aui-message-bar-delete-org");
        dialog.updateHeight();
    }
    
    dialog.isAttached = function() { return !AJS.$.isEmptyObject(dialog.popup.element);}
    
    dialog.show(); 
    dialog.updateHeight();
    
    return dialog;
}

function deleteOrganization(organizationId, organizationName) {
    confirmationDialog({
        header: "Deleting Account '" + organizationName + "'",
        body: jira.dvcs.connector.plugin.soy.confirmDelete({'organizationName': organizationName}),
        submitButtonLabel: "Delete",
        okAction: function (dialog) { deleteOrganizationInternal(dialog, organizationId, organizationName); }
        });
}

function deleteOrganizationInternal(dialog, organizationId, organizationName) {
    AJS.$.ajax({
        url: BASE_URL + "/rest/bitbucket/1.0/organization/" + organizationId,
        type: 'DELETE',
        success: function(result) {
            window.location.reload();
        }
    }).error(function (err) {
        dialog.showError("Error when deleting account '" + organizationName + "'.");
    });
}

function syncRepositoryList(organizationId,organizationName) {
    var dialog = new AJS.Dialog({width:400, height:150, id:"refreshing-account-dialog", closeOnOutsideClick: false});
    dialog.addHeader("Refreshing Account");
	dialog.addPanel("RefreshPanel", "<p>Refreshing '" + organizationName + "' account. Please wait... <span class='aui-icon aui-icon-wait'>&nbsp;</span></p>");
    dialog.show(); 
	dialog.updateHeight();
    
    AJS.$.ajax({
      url: BASE_URL + "/rest/bitbucket/1.0/organization/" + organizationId + "/syncRepoList",
        type: 'GET',
        success: function(result) {
            window.location.reload();
        }
    }).error(function (err) {
        showError("Error when refreshing account '" + organizationName + "'.", "#aui-message-bar-"+organizationId);
        dialog.remove();
    });
}

function showError(message, auiMessageElement, closeable) {
    if (typeof auiMessageElement == 'undefined') {
        auiMessageElement = "#aui-message-bar-global";
    }
        
    AJS.$(auiMessageElement).empty();
    AJS.messages.error(auiMessageElement, {
        title: message,
        closeable: closeable
    });
}

function setChecked(checkboxId, checked) {
    if (checked) {
        AJS.$("#" + checkboxId).attr("checked", "checked");
    } else {
        AJS.$("#" + checkboxId).removeAttr("checked");
    }
}

function setCheckedDropdown2(checkboxId, checked) {
    if (checked) {
        AJS.$("#" + checkboxId).addClass("checked");
    } else {
        AJS.$("#" + checkboxId).removeClass("checked");
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

function parseAccountUrl(url) {
    var pattern=/(.*)\/(.+?)\/?$/;
    var matches = url.match(pattern);
    if (matches)
        return {hostUrl:matches[1], name:matches[2]};
}

//------------------------------------------------------------

AJS.$(document).ready(function() {
    if (typeof init_repositories == 'function') {
        // cancel annoying leave message even when browser pre-fill some fields
        window.onbeforeunload = function () {};

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
