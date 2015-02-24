function switchDvcsDetails(selectSwitch) {
    var dvcsType = selectSwitch.selectedIndex;
    switchDvcsDetailsInternal(dvcsType);
}

function switchDvcsDetailsInternal(dvcsType) {
    // clear all form errors
    DvcsValidator.clearAllErrors();

    // impose real URL to hidden input
    AJS.$("#url").val(dvcsKnownUrls[AJS.$("#urlSelect option:selected").val()]);
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

    } else if (dvcsType == 2) {

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
            dialog = AJS.InlineDialog(AJS.$("#jira-dvcs-connector-forceSyncDialog-" + repositoryId), "jira-dvcs-connector-forceSyncDialog" + repositoryId, function (content, trigger, showPopup) {
                content.html(dvcs.connector.plugin.soy.forceSyncDialog({
                    'repositoryId':repositoryId
                }));
                showPopup();
                return false;
            }, { width:500, hideDelay:null, noBind:true });
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
        window.setTimeout(retrieveSyncStatus, 4000);
    }).fail(function( jqxhr, textStatus, error ) {
        var err = textStatus + ", " + error;
        dvcsLogConsole("Request Failed: " + err);
        window.setTimeout(retrieveSyncStatus, 40000);
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
            syncStatusHtml = getLastCommitRelativeDateHtml(repo.lastActivityDate);
            var title = syncRepoIconElement.attr("data-title");
            if (repo.sync.finishTime) {
              var finishSyncDateTime = new Date(repo.sync.finishTime);
              title += " (last sync finished at " + finishSyncDateTime.toDateString() + " " + finishSyncDateTime.toLocaleTimeString()  + ")";
            }
            syncRepoIconElement.attr("title", title);
            if (repo.sync.error) {
                syncStatusHtml = "";
                syncIcon = "error";
                syncErrorDiv.html("<span class=\"error\"><strong>Sync Failed:</strong> " + repo.sync.error + "</span>" +
                        "<span style='color:#000;'> &nbsp; &ndash; &nbsp;</span>");
            } else {
                syncErrorDiv.html("");
            }
        }/* else if (repo.sync.startTime === 0) {
            syncRepoIcon = "syncrepoiconqueue";
            syncRepoIconElement.attr("title", "In queue");
        }*/ else {
            syncErrorDiv.html("");
            var title = "Synchronizing...";
            if (repo.sync.startTime) {
               var startSyncDateTime = new Date(repo.sync.startTime);
                title += " (started at " + startSyncDateTime.toDateString() + " " + startSyncDateTime.toLocaleTimeString() + ")";
            }
            syncRepoIconElement.attr("title", title);
            syncRepoIcon = "running";
            syncStatusHtml = "Synchronizing: <strong>" + repo.sync.changesetCount + "</strong> changesets, <strong> " + repo.sync.pullRequestActivityCount + " </strong> pull requests, <strong>" + repo.sync.jiraCount + "</strong> issues found";
            if (repo.sync.synchroErrorCount > 0)
                syncStatusHtml += ", <span style='color:#e16161;'><strong>" + repo.sync.synchroErrorCount + "</strong> changesets incomplete</span>";
        }

        var errorSmrtcmmtIcon = AJS.$("#error_smrtcmmt_icon_" + repo.id);
        // show error icon if smart commit has error
        if (repo.sync.smartCommitErrors.length > 0) {
            errorSmrtcmmtIcon.addClass("error_smrtcmmt aui-icon aui-icon-small aui-iconfont-error dvcs-color-red");
            var tooltip = registerInlineDialogTooltip(errorSmrtcmmtIcon, dvcs.connector.plugin.soy.smartCommitErrors({'smartCommitErrors':repo.sync.smartCommitErrors}));
        } else {
            errorSmrtcmmtIcon.removeClass("error_smrtcmmt aui-icon aui-icon-small aui-iconfont-error dvcs-color-red")
        }

    }

    else {
    	if (repo.lastActivityDate)
        syncStatusHtml = getLastCommitRelativeDateHtml(repo.lastActivityDate);
    }
    syncIconElement.removeClass("commits").removeClass("finished").removeClass("running").removeClass("error").addClass(syncIcon);
    syncRepoIconElement.removeClass("running").removeClass("syncrepoiconqueue").addClass(syncRepoIcon);
    syncRepoIconElement.tooltip({aria:true});
    syncStatusDiv.html(syncStatusHtml);
}

function getLastCommitRelativeDateHtml(daysAgo) {
    var html = "";
    if (daysAgo) {
        html = new Date(daysAgo).toDateString();
    }
    return html;
}

function showAddRepoDetails(show, hostToSelect) {
    if (!dvcs.connector.plugin.addOrganizationDialog) {
        createAddOrganizationDialog();
    }
    var dialog = dvcs.connector.plugin.addOrganizationDialog;
    // Reset to default view:
    AJS.$('#repoEntry').attr("action", "");
    // - hide username/password
    AJS.$("#github-form-section").hide();

    // - show url, organization field
    var urlSelect = AJS.$('#urlSelect');
    urlSelect.show();
    /**
     * Building an internal map of all of the available hosts to avoid the use of AJS.$ or .find
     * in the case of potential XSS hole when mixing input from url with query string
     */
    var availableHosts = {};
    var defaultHost;
    urlSelect.find("option").each(function(index, option) {
        var $option = AJS.$(option);
        $option.data("index", index);
        if (!dvcs.connector.plugin.disabledHosts[$option.attr("value")]) {
            var host = $option.attr("value");
            availableHosts[host] = $option;
            if (!defaultHost || host == "bitbucket") {
                defaultHost = host;
            }
        }
    });

    var selectedHost;
    if (hostToSelect && availableHosts[hostToSelect]) {
        selectedHost = AJS.$(availableHosts[hostToSelect]);
    } else {
        //Defaults to bitbucket
        selectedHost = AJS.$(availableHosts[defaultHost]);
    }

    urlSelect.val(selectedHost.attr("value"));
    AJS.$('#urlReadOnly').hide();

    AJS.$('#organization').show();
    AJS.$('#organizationReadOnly').hide();

    dialog.enabled(true);

    // clear all form errors
    DvcsValidator.clearAllErrors();

    // Enable form for the selected host
    switchDvcsDetailsInternal(selectedHost.data("index"));

    AJS.$("#organization").focus().select();
    dialog.gotoPage(0);
    dialog.gotoPanel(0);
    dialog.show();
    dialog.updateHeight();

    triggerAnalyticsEvent("add.started");
}

function createAddOrganizationDialog(action) {
    var dialog = new AJS.Dialog({
        width:800,
        height:400,
        id:"add-organization-dialog",
        closeOnOutsideClick:false
    });

    // First page
    dialog.addHeader("Add New Account");

    dialog.addPanel("", dvcs.connector.plugin.soy.addOrganizationDialog({
        isOnDemandLicense:dvcs.connector.plugin.onDemandLicense,
        atlToken:dvcs.connector.plugin.atlToken,
        oAuthStore:dvcs.connector.plugin.oAuthStore,
        source: getSourceDiv().data("source"),
        disabledHosts: dvcs.connector.plugin.disabledHosts
    }), "panel-body");

    dialog.addButtonPanel();
    dialog.page[0].buttonpanel.append("<span id='add-organization-wait' class='aui-icon' style='padding-right:10px'>&nbsp;</span>");
    dialog.addSubmit("Add", function (dialog, event) {
        if (dvcsSubmitFormHandler(event, false)) {
            AJS.$("#repoEntry").submit();
        }
    });

    dialog.addCancel("Cancel", function (dialog) {
        AJS.$("#repoEntry").trigger('reset');
        AJS.$("#aui-message-bar").empty();
        dialog.hide();
        triggerAnalyticsEvent("add.cancelled");
    }, "#");

    AJS.$('#urlSelect').change(function (event) {
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

    dialog.addButton("Previous", function (dialog) {
        dialog.prevPage();
        dialog.updateHeight();
    });

    dialog.addCancel("Cancel", function (dialog) {
        AJS.$("#repoEntry").trigger('reset');
        AJS.$("#aui-message-bar").empty();
        dialog.hide();
        triggerAnalyticsEvent("add.cancelled");
    }, "#");

    dialog.enabled = function (enabled) {
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
    dvcs.connector.plugin.addOrganizationDialog = dialog;
}

function dvcsSubmitFormHandler(event, skipLoggingAlert) {
    var dialog = dvcs.connector.plugin.addOrganizationDialog;
    // submit form
    var organizationElement = AJS.$("#organization");
    // if not custom URL
    if (!parseAccountUrl(organizationElement.val())) {
        // some really simple validation
        if (!validateAddOrganizationForm()) {
            dialog.enabled(true);
            dialog.updateHeight();
            return false;
        }
        var selectedDvcs = AJS.$("#urlSelect option:selected");
        var dvcsHost = selectedDvcs.text();

        if (selectedDvcs.val() == "githube") { // Github Enterprise
            // impose real URL to hidden input
            AJS.$("#url").val(AJS.$("#urlGhe").val());

            if (!skipLoggingAlert) {
                AJS.$("#githubeConfirmation").html(dvcs.connector.plugin.soy.confirmLoggedIn({
                    dvcsHost:dvcsHost
                }));
                dialog.nextPage();
                dialog.updateHeight();
                return false;
            }
        }

        // disable add form
        dialog.enabled(false);

        //
        AJS.messages.info("#aui-message-bar", { title:"Connecting to " + dvcsHost + " to configure your account...", closeable:false});
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

    AJS.messages.info("#aui-message-bar", { title:"Trying to identify repository type...", closeable:false});
    dialog.updateHeight();

    var repositoryUrl = AJS.$("#url").val().trim();
    var organizationName = AJS.$("#organization").val().trim();

    var requestUrl = BASE_URL + "/rest/bitbucket/1.0/accountInfo?server=" + encodeURIComponent(repositoryUrl) + "&account=" + encodeURIComponent(organizationName);

    AJS.$.getJSON(requestUrl,
        function (data) {

            AJS.$("#aui-message-bar").empty();
            dialog.enabled(true);

            if (data.validationErrors && data.validationErrors.length > 0) {
                AJS.$.each(data.validationErrors, function (i, msg) {
                    AJS.messages.error("#aui-message-bar", {title:"Error!", body:msg});
                    dialog.updateHeight();
                })
            } else {
                dvcsSubmitFormAjaxHandler[data.dvcsType].apply(this, arguments);
            }
        }).error(function (a) {
            AJS.$("#aui-message-bar").empty();
            AJS.messages.error("#aui-message-bar", { title:"Error!",
                body:"The url [<b>" + AJS.escapeHtml(AJS.$("#url").val()) + "</b>] is incorrect or the server is not responding."
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
        validator.addItem("urlGhe", "ghe-url-error", "required");
        validator.addItem("urlGhe", "ghe-invalid-url-error", "url");
        validator.addItem("oauthClientIdGhe", "oauth-ghe-client-error", "required");
        validator.addItem("oauthSecretGhe", "oauth-ghe-secret-error", "required");
    } else if (AJS.$("#oauthBbClientId").is(":visible")) {
        validator.addItem("oauthBbClientId", "oauth-bb-client-error", "required");
        validator.addItem("oauthBbSecret", "oauth-bb-secret-error", "required");
    } else if (AJS.$("#oauthClientId").is(":visible")) {
        validator.addItem("oauthClientId", "oauth-gh-client-error", "required");
        validator.addItem("oauthSecret", "oauth-gh-secret-error", "required");
    } else if (AJS.$("#adminUsername").is(":visible")) {
        // validator.addItem("adminUsername", "admin-username-error", "required");
        // validator.addItem("adminPassword", "admin-password-error", "required");
    }
    return validator.runValidation();
}

var dvcsSubmitFormAjaxHandler = {
    "bitbucket":function (data) {
        AJS.$("#repoEntry").attr("action", BASE_URL + "/secure/admin/AddBitbucketOrganization.jspa");
        AJS.$('#repoEntry').submit();
    },

    "github":function (data) {
        AJS.$("#repoEntry").attr("action", BASE_URL + "/secure/admin/AddGithubOrganization.jspa");
        AJS.$('#repoEntry').submit();
    }
}

function configureDefaultGroups(orgName, id) {

    var dialog = confirmationDialog({
        header:"Configure automatic access",
        body:dvcs.connector.plugin.soy.defaultGroupsForm({
            'baseUrl':BASE_URL,
            'atlToken':dvcs.connector.plugin.atlToken,
            'organizationIdDefaultGroups':id
        }),
        submitButtonLabel:"Save",
        okAction:function (dialog) { AJS.$("#configureDefaultGroupsForm").submit();}
    });

    dialog.page[0].buttonpanel.append("<span class='dialog-help'>Help on <a href='https://confluence.atlassian.com/x/Bw4zDQ' target='_blank'>account management</a></span>");
    dialog.disableActions();

    // load web fragment
    AJS.$.ajax({
            type:'GET',
            url : BASE_URL + "/rest/bitbucket/1.0/organization/" + id + "/defaultgroups",
            success:
            function (data) {
                if (dialog.isAttached()) {
	                AJS.$(".dialog-panel-body #configureDefaultGroupsContent").html(dvcs.connector.plugin.soy.defaultGroups({
	                	organization: data.organization,
	                	groups: data.groups
	                }));
                    dialog.updateHeight();
                    dialog.enableActions();
                }
            }
        }
    ).error(function (err) {
    	dialog.showError(AJS.$(err.responseXML).find('status').find('message').text());
        dialog.updateHeight();
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
        width:600,
        height:350,
        id:"repositoryOAuthDialog"
    });

    popup.addHeader("Configure OAuth for account " + org.name);
    popup.addPanel("", dvcs.connector.plugin.soy.repositoryOAuthDialog({
        'organizationId':org.id,
        'oAuthKey':org.credential.key,
        'oAuthSecret':org.credential.secret,
        'isOnDemandLicense':dvcs.connector.plugin.onDemandLicense
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
            .done(function (data) {

                var actionName;
                if (org.dvcsType == "bitbucket")
                    actionName = "RegenerateBitbucketOauthToken.jspa";
                else if (org.dvcsType == "github")
                    actionName = "RegenerateGithubOauthToken.jspa";
                else
                    actionName = "RegenerateGithubEnterpriseOauthToken.jspa";

                window.location.replace(BASE_URL + "/secure/admin/" + actionName + "?organization=" + org.id + "&atl_token=" + atlToken);
            })
            .error(function (err) {
                AJS.$("#aui-message-bar-oauth-dialog").empty();
                AJS.messages.error("#aui-message-bar-oauth-dialog", { title:"Error!",
                    body:"Could not configure OAuth.",
                    closeable:false
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

    AJS.$.getJSON(BASE_URL + "/rest/bitbucket/1.0/organization/" + org.id + "/tokenOwner",function (data) {
    	if (data.fullName.replace(/\s+/g, '').length == 0) 
    		data.fullName = data.username;
        AJS.$(".repositoryOAuthDialog #tokenUser").html(dvcs.connector.plugin.soy.repositoryOAuthDialogTokenOwner(data));
    }).error(function (err) {
            AJS.$(".repositoryOAuthDialog #tokenUser").html("<i>&lt;Invalid, please regenerate access token.&gt;<i>");
        });

    return false;
}

function setOAuthSettings(org) {
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
        width:600,
        height:350,
        id:"OAuthSettingsDialog"
    });

    popup.addHeader("OAuth settings for account " + org.name);
    popup.addPanel("", dvcs.connector.plugin.soy.OAuthSettingsDialog({
        'organizationId':org.id,
        'oAuthKey':org.credential.key,
        'oAuthSecret':org.credential.secret,
        'isOnDemandLicense':dvcs.connector.plugin.onDemandLicense
    }));

    clearError(AJS.$("#updateOAuthForm #key"));
    clearError(AJS.$("#updateOAuthForm #secret"));
    popup.addButton("Save", function (dialog) {
        // validate
        var v1 = validateField(AJS.$("#updateOAuthForm #key"), "OAuth key must not be blank");
        var v2 = validateField(AJS.$("#updateOAuthForm #secret"), "OAuth secret must not be blank");
        popup.updateHeight();
        if (!v1 || !v2) return;

        AJS.$("#OAuthSettingsDialog .dialog-button-panel button").attr("disabled", "disabled");
        AJS.$("#OAuthSettingsDialog .dialog-button-panel button").attr("aria-disabled", "true");
        AJS.$("#OAuthSettingsDialog .dialog-button-panel").prepend("<span class='aui-icon aui-icon-wait' style='padding-right:10px'>Wait</span>");

        // submit form
        AJS.$.post(BASE_URL + "/rest/bitbucket/1.0/org/" + org.id + "/oauth", AJS.$("#updateOAuthForm").serialize())
            .done(function (data) {
                popup.hide();
                syncRepositoryList(org.id, org.name);
            })
            .error(function (err) {
                AJS.$("#aui-message-bar-oauth-dialog").empty();
                AJS.messages.error("#aui-message-bar-oauth-dialog", { title:"Error!",
                    body:"Could not configure OAuth.",
                    closeable:false
                });
                AJS.$("#OAuthSettingsDialog .dialog-button-panel button").removeAttr("disabled", "disabled");
                AJS.$("#OAuthSettingsDialog .dialog-button-panel button").removeAttr("aria-disabled");
                AJS.$("#OAuthSettingsDialog .dialog-button-panel .aui-icon-wait").remove();
                popup.updateHeight();
            });
    }, "aui-button submit");

    popup.addCancel("Cancel", function (dialog) {
        dialog.remove();
    });

    popup.show();
    popup.updateHeight();

    return false;
}

function registerDropdownCheckboxHandlers() {
    AJS.$("a[id^='org_autolink_check']").on({
        "aui-dropdown2-item-check":function () {
            autoLinkIssuesOrg(this.id.substring("org_autolink_check".length), this.id, true);
        },
        "aui-dropdown2-item-uncheck":function () {
            autoLinkIssuesOrg(this.id.substring("org_autolink_check".length), this.id, false);
        }
    });

    AJS.$("a[id^='org_global_smarts']").on({
        "aui-dropdown2-item-check":function () {
            enableSmartcommitsOnNewRepos(this.id.substring("org_global_smarts".length), this.id, true);
        },
        "aui-dropdown2-item-uncheck":function () {
            enableSmartcommitsOnNewRepos(this.id.substring("org_global_smarts".length), this.id, false);
        }
    });
}

function autoLinkIssuesOrg(organizationId, checkboxId, checkedValue) {
    AJS.$("#" + checkboxId).addClass("disabled");
    AJS.$.ajax({
            type:'POST',
            dataType:"json",
            contentType:"application/json",
            url:BASE_URL + "/rest/bitbucket/1.0/org/" + organizationId + "/autolink",
            data:'{ "payload" : "' + checkedValue + '"}',
            success: 
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
            showError("Unexpected error occurred. Please contact the server administrator.", "#aui-message-bar-" + organizationId);
            AJS.$("#" + checkboxId).removeClass("disabled");
            setCheckedDropdown2(checkboxId, !checkedValue);
        });
}

function enableSmartcommitsOnNewRepos(organizationId, checkboxId, checkedValue) {
    AJS.$("#" + checkboxId).addClass("disabled");
    AJS.$.ajax({
            type:'POST',
            dataType:"json",
            contentType:"application/json",
            url:BASE_URL + "/rest/bitbucket/1.0/org/" + organizationId + "/globalsmarts",
            data:'{ "payload" : "' + checkedValue + '"}',
            success:
            function (data) {
                AJS.$("#" + checkboxId).removeClass("disabled");
            }
        }
    ).error(function (err) {
            showError("Unexpected error occurred when enabling smart commits on new repositories. Please contact the server administrator.", "#aui-message-bar-" + organizationId);
            AJS.$("#" + checkboxId).removeClass("disabled");
            setCheckedDropdown2(checkboxId, !checkedValue);
        });
}

function autoLinkIssuesRepo(repoId, checkboxId) {
    var checkedValue = AJS.$("#" + checkboxId).is(":checked");
    AJS.$("#" + checkboxId).attr("disabled", "disabled");
    AJS.$("#" + checkboxId + "working").show();
    AJS.$.ajax( 
    	{
            type:'POST',
            dataType:"json",
            contentType:"application/json",
            url:BASE_URL + "/rest/bitbucket/1.0/repo/" + repoId + "/autolink",
            data:'{ "payload" : "' + checkedValue + '"}',

            success:
            function (registration) {
                if (registration.callBackUrlInstalled != checkedValue) {

                    var popup = new AJS.Dialog({
                        width:600,
                        height:400,
                        id:"dvcs-postcommit-hook-registration-dialog",
                        closeOnOutsideClick:false
                    });

                    popup.addHeader((checkedValue ? "Linking" : "Unlinking") + " the repository");
                    popup.addPanel("Registration", dvcs.connector.plugin.soy.postCommitHookDialog({
                        'registering':checkedValue,
                        'callbackUrl':registration.callBackUrl
                    }));
                    popup.addButton("OK", function (dialog) {
                        popup.remove();
                    }, "aui-button submit");
                    popup.show();
                    popup.updateHeight();

                    // show warning icon if not already shown
                    var errorStatusIcon = AJS.$("#error_status_icon_" + repoId);
                    errorStatusIcon.addClass("admin_permission aui-icon aui-icon-small aui-iconfont-warning dvcs-color-yellow");
                    registerAdminPermissionInlineDialogTooltip(errorStatusIcon);
                }

                AJS.$("#" + checkboxId + "working").hide();
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
            var errorStatusIcon = AJS.$("#error_status_icon_" + repoId);
            errorStatusIcon.removeClass("admin_permission aui-iconfont-warning dvcs-color-yellow").addClass("aui-icon aui-icon-small aui-iconfont-error dvcs-color-red");
            var response = AJS.$.parseJSON(err.responseText);
            var message = "";
            if (response) {
                message = response.message;
            }
            var tooltip = registerInlineDialogTooltip(errorStatusIcon, dvcs.connector.plugin.soy.linkingUnlinkingError({'isLinking':checkedValue, 'errorMessage':message}));
            tooltip.show();
            AJS.$("#" + checkboxId + "working").hide();
            AJS.$("#" + checkboxId).removeAttr("disabled");
            setChecked(checkboxId, !checkedValue);
        });
}

function registerAdminPermissionInlineDialogTooltips() {
    AJS.$(".admin-permission").each(function (index) {
        registerAdminPermissionInlineDialogTooltip(this);
    });
}

function registerAdminPermissionInlineDialogTooltip(element) {
    registerInlineDialogTooltip(element, dvcs.connector.plugin.soy.adminPermisionWarning());
}

function registerInlineDialogTooltip(element, body) {


    var inlineDialogContent = AJS.$(element).data("inlineDialogContent");
    AJS.$(element).data("inlineDialogContent", body);
    if (inlineDialogContent) { // inline dialog is already registered
        return;
    }
    
    return AJS.InlineDialog(AJS.$(element), "tooltip_" + AJS.$(element).attr('id'),
        function (content, trigger, showPopup) {
            var inlineDialogContent = AJS.$(element).data("inlineDialogContent");
            content.css({"padding":"20px", "width":"auto"}).html(inlineDialogContent);
            showPopup();
            return false;
        }, 
        {onHover:true, hideDelay:200, showDelay:1000, arrowOffsetX:-8, offsetX:-80}
    );
}

function enableRepoSmartcommits(repoId, checkboxId) {
    var checkedValue = AJS.$("#" + checkboxId).is(":checked");
    AJS.$("#" + checkboxId).attr("disabled", "disabled");
    AJS.$("#" + checkboxId + "working").show();
    AJS.$.ajax(
    	{
            type:'POST',
            dataType:"json",
            contentType:"application/json",
            url:BASE_URL + "/rest/bitbucket/1.0/repo/" + repoId + "/smart",
            data:'{ "payload" : "' + checkedValue + '"}',

            success :
            function (data) {
                AJS.$("#" + checkboxId + "working").hide();
                AJS.$("#" + checkboxId).removeAttr("disabled");
            }
        }
    ).error(function (err) {
            showError("Unexpected error occurred.");
            AJS.$("#" + checkboxId + "working").hide();
            AJS.$("#" + checkboxId).removeAttr("disabled");
            setChecked(checkboxId, !checkedValue);
        });
}

function confirmationDialog(options) {
    var dialog = new AJS.Dialog({width:500, height:150, id:"confirm-dialog", closeOnOutsideClick:false});
    dialog.addHeader(options.header);
    dialog.addPanel("ConfirmPanel", options.body + "<div id='aui-message-bar-confirmation-dialog'></div>");

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

    dialog.disableActions = function () {
        AJS.$('#confirm-dialog .button-panel-submit-button').attr("disabled", "disabled");
        AJS.$('#confirm-dialog .button-panel-submit-button').attr("aria-disabled", "true");
        AJS.$('#confirm-dialog .button-panel-cancel-link').addClass('dvcs-link-disabled');
    }

    dialog.enableActions = function () {
        AJS.$('#confirm-dialog .button-panel-submit-button').removeAttr("disabled");
        AJS.$('#confirm-dialog .button-panel-submit-button').removeAttr("aria-disabled");
        AJS.$('#confirm-dialog .button-panel-cancel-link').removeClass('dvcs-link-disabled');
    }

    dialog.working = function (working) {
        if (working) {
            AJS.$("#confirm-action-wait").addClass("aui-icon-wait");
            this.disableActions();
        } else {
            AJS.$("#confirm-action-wait").removeClass("aui-icon-wait");
            this.enableActions();
        }
    }

    dialog.showError = function (message) {
        dialog.working(false);
        showError(message, "#aui-message-bar-confirmation-dialog");
        dialog.updateHeight();
    }

    dialog.isAttached = function () { return !AJS.$.isEmptyObject(dialog.popup.element);}

    dialog.show();
    dialog.updateHeight();

    return dialog;
}

function deleteOrganization(organizationId, organizationName) {
    confirmationDialog({
        header:"Deleting Account '" + organizationName + "'",
        body:dvcs.connector.plugin.soy.confirmDelete({'organizationName':organizationName}),
        submitButtonLabel:"Delete",
        okAction:function (dialog) { deleteOrganizationInternal(dialog, organizationId, organizationName);}
    });
}

function deleteOrganizationInternal(dialog, organizationId, organizationName) {
    AJS.$.ajax({
        url:BASE_URL + "/rest/bitbucket/1.0/organization/" + organizationId,
        type:'DELETE',
        timeout:5 * 60 * 1000,
        success:function (result) {
            AJS.$("#dvcs-orgdata-container-" + organizationId).remove();
            dialog.remove();
        }
    }).error(function (jqXHR, textStatus, errorThrown) {
    	// ignore not found status
    	if (jqXHR.status == 404) {
    		AJS.$("#dvcs-orgdata-container-" + organizationId).remove();
    		dialog.showError("Account '" + organizationName + "' was already deleted!");
    	} else if(jqXHR.status == 0) { // timeout can happen if organization has too many repositories and uninstalling postcommit hooks takes too long. Pretend deletion is succesfull (it will continue on the server)
            AJS.$("#dvcs-orgdata-container-" + organizationId).remove();
            dialog.remove();
    	} else {
    		dialog.showError("Error when deleting account '" + organizationName + "'.");
    	}
    });
}

function syncRepositoryList(organizationId, organizationName) {
    var dialog = new AJS.Dialog({width:400, height:150, id:"refreshing-account-dialog", closeOnOutsideClick:false});
    dialog.addHeader("Refreshing Account");
    dialog.addPanel("RefreshPanel", "<p>Refreshing '" + organizationName + "' account. Please wait... <span class='aui-icon aui-icon-wait'>&nbsp;</span></p>");
    dialog.show();
    dialog.updateHeight();

    AJS.$.ajax({
        url:BASE_URL + "/rest/bitbucket/1.0/organization/" + organizationId + "/syncRepoList",
        type:'GET',
        success:function (result) {
            window.location.reload();
        }
    }).error(function (err) {
            showError("Error when refreshing account '" + organizationName + "'.", "#aui-message-bar-" + organizationId);
            dialog.remove();
        });
}

function showError(message, auiMessageElement, closeable) {
    if (typeof auiMessageElement == 'undefined') {
        auiMessageElement = "#aui-message-bar-global";
    }

    AJS.$(auiMessageElement).empty();
    AJS.messages.error(auiMessageElement, {
        title:message,
        closeable:closeable
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
    "bitbucket":"https://bitbucket.org",
    "github":"https://github.com",
    "githube":"https://github.com"
};

function parseAccountUrl(url) {
    var pattern = /(.*)\/(.+?)\/?$/;
    var matches = url.match(pattern);
    if (matches)
        return {hostUrl:matches[1], name:matches[2]};
}

function getSourceDiv()
{
    return AJS.$("#dvcs-connect-source");
}

function triggerAnalyticsEvent(eventType) {
    if (AJS.EventQueue) {
        var source = getSourceDiv().data("sourceOrDefault");
        AJS.EventQueue.push({name: "jira.dvcsconnector.config." + eventType + "." + source});
    }
}

//------------------------------------------------------------

AJS.$(document).ready(function () {
    if (typeof init_repositories == 'function') {
        // cancel annoying leave message even when browser pre-fill some fields
        window.onbeforeunload = function () {};

        // defined in macro
        init_repositories();

        /**
         * DVCS connector uses the hash '#expand' in the URL to determine whether to automatically open the
         * 'Add New Account' dialog.
         */
        if (window.location.hash == '#expand') {
            var hostToSelect = undefined;
            if (parseUri) {
                //queryKey should always be available in the object returned by parseUri(), but it's good to be defensive anyway
                var urlQueries = parseUri(window.location.href).queryKey || {};
                hostToSelect = urlQueries.selectHost;
            }
            showAddRepoDetails(true, hostToSelect);
        }
    }
});

function dvcsLogConsole(msg) {
    if ( window.console && window.console.log ) {
       console.log("DVCS: " + msg);
    }
}
//---------------------------------------------------------

AJS.$.fn.extend({
    dvcsGearMenu:function (opts) {
        // original AUI dropdown
        this.dropDown();
        // stop further propagation - causes not hide dropdown menu
        AJS.$(opts.noHideItemsSelector).bind("click", function (e) {
            e.stopPropagation();
        });
    }
});
