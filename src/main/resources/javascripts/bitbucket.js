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

function submitFunction(a) {
    var repositoryUrl = AJS.$("#url").val().trim();
    var privateBbCredentialsVisible = AJS.$("#privateBbCredentialsVisible").val();
    //var atlToken = AJS.$("#atl_token").val();

    var requestUrl = BASE_URL + "/rest/bitbucket/1.0/urlinfo?repositoryUrl=" + encodeURIComponent(repositoryUrl)

    AJS.$("#aui-message-bar").empty();
    AJS.messages.hint({
        title: "Working...",
        body: "Trying to connect to the repository."
    });

    AJS.$.getJSON(requestUrl,
        function(data) {
            AJS.$("#aui-message-bar").empty();
            AJS.$("#isPrivate").val(data.isPrivate);

            if (data.repositoryType == "github")
                AJS.$("#repoEntry").attr("action", BASE_URL + "/secure/admin/AddGithubRepository.jspa");
            else if (data.repositoryType == "bitbucket")
                AJS.$("#repoEntry").attr("action", BASE_URL + "/secure/admin/AddBitbucketRepository.jspa");

            if (privateBbCredentialsVisible == "false" && data.repositoryType == "bitbucket" && data.isPrivate) {
                AJS.$("#privateBbCredentialsVisible").val("true");

                var credentialsHtml = "<h3>For private Bitbucket repository you have to add access credentials</h3>" +
                    "<div class='field-group'>" +
                    "<label for='bbUsername'>Username</label>" +
                    "<input type='text' name='bbUsername' id='bbUsername' value=''></div>" +
                    "<div class='field-group' style='margin-bottom: 10px;'>" +
                    "<label for='bbPassword'>Password</label>" +
                    "<input type='password' name='bbPassword' id='bbPassword' value=''></div>";
                AJS.$("#bbCredentials").html(credentialsHtml);
            } else if (data.validationError) {
				AJS.messages.error({
					title : "Error!",
					body : data.validationError
				});
			} else {
                AJS.$('#repoEntry').submit();
            }
        }).error(function(a)
        {
            AJS.$("#aui-message-bar").empty();
            AJS.messages.error({
                title: "Error!",
                body: "The repository url [<b>" + AJS.$("#url").val() + "</b>] is incorrect or the repository is not responding."
            });
        });
    return false;
}

function showAddRepoDetails(show) {
    if (show) {
        AJS.$('#linkRepositoryButton').fadeOut(function() {
            AJS.$('#addRepositoryDetails').slideDown();
        });
    } else {
        AJS.$('#addRepositoryDetails').slideUp(function() {
            AJS.$('#linkRepositoryButton').fadeIn();
            AJS.$("#bbCredentials").html("");
        });
    }
}

AJS.$(document).ready(function() {
    if (typeof init_repositories == 'function') {
        init_repositories();
    }

})


